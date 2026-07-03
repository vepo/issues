package dev.vepo.issues.ticket.csvimport;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import dev.vepo.issues.categories.CategoryRepository;
import dev.vepo.issues.project.Project;
import dev.vepo.issues.project.ProjectRepository;
import dev.vepo.issues.ticket.Ticket;
import dev.vepo.issues.ticket.TicketPriority;
import dev.vepo.issues.ticket.TicketRepository;
import dev.vepo.issues.ticket.TicketResponse;
import dev.vepo.issues.user.UserRepository;
import dev.vepo.issues.workflow.WorkflowStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class TicketImportService {

    private static final int TITLE_MIN = 5;
    private static final int TITLE_MAX = 255;
    private static final int DESCRIPTION_MIN = 5;
    private static final int DESCRIPTION_MAX = 1200;
    private static final int SAMPLE_ROW_LIMIT = 5;

    private final TicketImportRepository importRepository;
    private final TicketImportRowRepository importRowRepository;
    private final ProjectRepository projectRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final CsvImportParser csvImportParser;
    private final TicketImportJson importJson;
    private final TicketImportRowExecutor rowExecutor;
    private final TicketRepository ticketRepository;

    @Inject
    public TicketImportService(TicketImportRepository importRepository,
                               TicketImportRowRepository importRowRepository,
                               ProjectRepository projectRepository,
                               CategoryRepository categoryRepository,
                               UserRepository userRepository,
                               CsvImportParser csvImportParser,
                               TicketImportJson importJson,
                               TicketImportRowExecutor rowExecutor,
                               TicketRepository ticketRepository) {
        this.importRepository = importRepository;
        this.importRowRepository = importRowRepository;
        this.projectRepository = projectRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.csvImportParser = csvImportParser;
        this.importJson = importJson;
        this.rowExecutor = rowExecutor;
        this.ticketRepository = ticketRepository;
    }

    @Transactional
    public TicketImportUploadResponse upload(Long projectId, String fileName, InputStream content, String username) {
        var author = userRepository.findByUsername(username)
                                   .orElseThrow(() -> new NotFoundException("User does not found! username=%s".formatted(username)));
        var parsed = csvImportParser.parse(content);
        if (parsed.rows().isEmpty()) {
            throw new BadRequestException("CSV file has no data rows");
        }

        var ticketImport = new TicketImport();
        if (projectId != null) {
            ticketImport.setProject(requireProject(projectId));
        }
        ticketImport.setAuthor(author);
        ticketImport.setFileName(fileName);
        ticketImport.setHeadersJson(importJson.writeHeaders(parsed.headers()));
        ticketImport.setStatus(TicketImportStatus.UPLOADED);
        ticketImport.setRowCount(parsed.rows().size());
        ticketImport.setTruncated(parsed.truncated());

        for (var parsedRow : parsed.rows()) {
            var row = new TicketImportRow();
            row.setRowNumber(parsedRow.rowNumber());
            row.setRawValuesJson(importJson.writeRawValues(parsedRow.values()));
            ticketImport.addRow(row);
        }

        importRepository.save(ticketImport);

        var sampleRows = parsed.rows()
                               .stream()
                               .limit(SAMPLE_ROW_LIMIT)
                               .map(CsvImportParser.ParsedCsvRow::values)
                               .toList();

        return new TicketImportUploadResponse(ticketImport.getId(),
                                              ticketImport.getFileName(),
                                              parsed.headers(),
                                              ticketImport.getRowCount(),
                                              ticketImport.isTruncated(),
                                              sampleRows,
                                              ticketImport.isProjectScoped());
    }

    @Transactional
    public void applyMapping(Long projectId, long importId, ColumnMapping mapping) {
        var ticketImport = requireImport(projectId, importId);
        if (ticketImport.getStatus() == TicketImportStatus.COMPLETED) {
            throw new BadRequestException("Import already completed");
        }
        validateMapping(ticketImport, mapping, importJson.readHeaders(ticketImport.getHeadersJson()));
        ticketImport.applyColumnMapping(mapping);
        ticketImport.setStatus(TicketImportStatus.MAPPED);

        for (var row : importRowRepository.findByImportId(importId)) {
            var values = importJson.readRawValues(row.getRawValuesJson());
            var mapped = mapRow(ticketImport, row.getRowNumber(), values, mapping);
            row.setTitle(mapped.title());
            row.setDescription(mapped.description());
            row.setCategoryName(mapped.categoryName());
            row.setProjectName(mapped.projectName());
            row.setPriority(mapped.priority());
            row.setAssigneeEmail(mapped.assigneeEmail());
            row.setStatusName(mapped.statusName());
            row.setValid(null);
            row.setValidationErrorsJson(null);
            row.setImportError(null);
        }

        importRepository.merge(ticketImport);
    }

    @Transactional
    public PreviewTicketImportResponse preview(Long projectId, long importId) {
        var ticketImport = requireImport(projectId, importId);
        ensureMapped(ticketImport);
        var validations = new ArrayList<ImportRowValidationResponse>();

        for (var row : importRowRepository.findByImportId(importId)) {
            validations.add(validateAndPersistRow(ticketImport, row));
        }

        var validCount = (int) validations.stream().filter(ImportRowValidationResponse::valid).count();
        return new PreviewTicketImportResponse(validations, validCount, validations.size() - validCount);
    }

    @Transactional
    public ImportRowValidationResponse correctRow(Long projectId, long importId, long rowId, CorrectImportRowRequest request) {
        var ticketImport = requireImport(projectId, importId);
        if (ticketImport.getStatus() == TicketImportStatus.COMPLETED) {
            throw new BadRequestException("Import already completed");
        }
        ensureMapped(ticketImport);
        var row = importRowRepository.findByIdAndImportId(rowId, importId)
                                     .orElseThrow(() -> new NotFoundException("Import row does not found! rowId=%d".formatted(rowId)));
        if (request.projectName() != null) {
            row.setProjectName(blankToNull(request.projectName()));
        }
        if (request.statusName() != null) {
            row.setStatusName(blankToNull(request.statusName()));
        }
        if (request.assigneeEmail() != null) {
            row.setAssigneeEmail(blankToNull(request.assigneeEmail()));
        }
        row.setValid(null);
        row.setValidationErrorsJson(null);
        return validateAndPersistRow(ticketImport, row);
    }

    @Transactional
    public ImportTicketsResponse execute(Long projectId, long importId, String username) {
        var ticketImport = requireImport(projectId, importId);
        ensureMapped(ticketImport);

        var created = new ArrayList<TicketResponse>();
        var importedTickets = new ArrayList<Ticket>();
        var errors = new ArrayList<ImportRowError>();

        for (var row : importRowRepository.findByImportId(importId)) {
            var validation = validateAndPersistRow(ticketImport, row);
            if (!validation.valid()) {
                errors.add(new ImportRowError(row.getRowNumber(), String.join("; ", validation.errors())));
                continue;
            }
            try {
                var rowProjectId = resolveProjectId(ticketImport, row);
                var ticketResponse = rowExecutor.importRow(rowProjectId, row.toMappedImportRow(), username);
                var ticket = ticketRepository.findById(ticketResponse.id()).orElseThrow();
                row.setTicket(ticket);
                row.setImportError(null);
                created.add(ticketResponse);
                importedTickets.add(ticket);
            } catch (RuntimeException ex) {
                row.setImportError(ex.getMessage());
                errors.add(new ImportRowError(row.getRowNumber(), ex.getMessage()));
            }
        }

        ticketImport.setStatus(TicketImportStatus.COMPLETED);
        importRepository.merge(ticketImport);

        return new ImportTicketsResponse(List.copyOf(created), List.copyOf(errors), buildSummary(importedTickets));
    }

    private ImportTicketsSummary buildSummary(List<Ticket> tickets) {
        var byProject = new java.util.LinkedHashMap<String, Integer>();
        var byStatus = new java.util.LinkedHashMap<String, Integer>();
        for (var ticket : tickets) {
            byProject.merge(ticket.getProject().getName(), 1, Integer::sum);
            byStatus.merge(ticket.getStatus().getName(), 1, Integer::sum);
        }
        return new ImportTicketsSummary(tickets.size(),
                                        byProject.size(),
                                        byProject.entrySet()
                                                 .stream()
                                                 .map(entry -> new ImportCountByName(entry.getKey(), entry.getValue()))
                                                 .toList(),
                                        byStatus.entrySet()
                                                .stream()
                                                .map(entry -> new ImportCountByName(entry.getKey(), entry.getValue()))
                                                .toList());
    }

    public MappedImportRow mapRow(TicketImport ticketImport, int rowNumber, Map<String, String> values, ColumnMapping mapping) {
        var projectName = ticketImport.isProjectScoped() ? null : blankToNull(cellValue(values, mapping.projectColumn()));
        return new MappedImportRow(rowNumber,
                                   cellValue(values, mapping.titleColumn()),
                                   cellValue(values, mapping.descriptionColumn()),
                                   cellValue(values, mapping.categoryColumn()),
                                   parsePriority(cellValue(values, mapping.priorityColumn())),
                                   blankToNull(cellValue(values, mapping.assigneeEmailColumn())),
                                   blankToNull(cellValue(values, mapping.statusColumn())),
                                   projectName);
    }

    private ImportRowValidationResponse validateAndPersistRow(TicketImport ticketImport, TicketImportRow row) {
        var mapped = row.toMappedImportRow();
        List<String> errors;
        boolean valid;
        if (row.getValid() != null && row.getValidationErrorsJson() != null) {
            valid = row.getValid();
            errors = importJson.readErrors(row.getValidationErrorsJson());
        } else {
            errors = collectValidationErrors(ticketImport, mapped);
            valid = errors.isEmpty();
            row.setValid(valid);
            row.setValidationErrorsJson(importJson.writeErrors(errors));
        }
        return new ImportRowValidationResponse(row.getId(), row.getRowNumber(), valid, mapped, List.copyOf(errors));
    }

    private List<String> collectValidationErrors(TicketImport ticketImport, MappedImportRow row) {
        var errors = new ArrayList<String>();

        validateTitle(row.title(), errors);
        validateDescription(row.description(), errors);

        if (resolveProject(ticketImport, row).isEmpty()) {
            if (ticketImport.isProjectScoped()) {
                errors.add("Project is required");
            } else if (isBlank(row.projectName())) {
                errors.add("Project is required");
            } else {
                errors.add("Unknown project: %s".formatted(row.projectName()));
            }
        }

        var project = resolveProject(ticketImport, row).orElse(null);

        if (isBlank(row.categoryName())) {
            errors.add("Category is required");
        } else if (categoryRepository.findByName(row.categoryName()).isEmpty()) {
            errors.add("Unknown category: %s".formatted(row.categoryName()));
        }

        if (row.priority() == null) {
            errors.add("Invalid priority value");
        }

        if (!isBlank(row.assigneeEmail()) && userRepository.findByEmail(row.assigneeEmail()).isEmpty()) {
            errors.add("Unknown assignee email: %s".formatted(row.assigneeEmail()));
        }

        if (project != null && !isBlank(row.statusName())) {
            var targetStatus = resolveStatusInProject(project, row.statusName()).orElse(null);
            if (targetStatus == null) {
                errors.add("Status not in project workflow: %s".formatted(row.statusName()));
            } else {
                var start = project.getWorkflow().getStart();
                if (!Objects.equals(start.getId(), targetStatus.getId())
                        && !hasDirectTransition(project, start, targetStatus)) {
                    errors.add("No direct transition from start to %s".formatted(targetStatus.getName()));
                }
            }
        }

        return errors;
    }

    private void validateMapping(TicketImport ticketImport, ColumnMapping mapping, List<String> headers) {
        if (isBlank(mapping.titleColumn()) || isBlank(mapping.descriptionColumn()) || isBlank(mapping.categoryColumn())) {
            throw new BadRequestException("Title, description and category mappings are required");
        }
        if (ticketImport.isProjectScoped()) {
            if (!isBlank(mapping.projectColumn())) {
                throw new BadRequestException("Project column mapping is not allowed for project-scoped imports");
            }
        } else if (isBlank(mapping.projectColumn())) {
            throw new BadRequestException("Project column mapping is required");
        }

        var requiredColumns = new ArrayList<String>();
        requiredColumns.add(mapping.titleColumn());
        requiredColumns.add(mapping.descriptionColumn());
        requiredColumns.add(mapping.categoryColumn());
        if (!ticketImport.isProjectScoped()) {
            requiredColumns.add(mapping.projectColumn());
        }
        for (var required : requiredColumns) {
            if (!headers.contains(required)) {
                throw new BadRequestException("Unknown CSV column: %s".formatted(required));
            }
        }
    }

    private long resolveProjectId(TicketImport ticketImport, TicketImportRow row) {
        return resolveProject(ticketImport, row.toMappedImportRow()).orElseThrow(() -> new BadRequestException("Project is required")).getId();
    }

    private Optional<Project> resolveProject(TicketImport ticketImport, MappedImportRow row) {
        if (ticketImport.isProjectScoped()) {
            return Optional.of(ticketImport.getProject());
        }
        if (isBlank(row.projectName())) {
            return Optional.empty();
        }
        return projectRepository.findByNameIgnoreCase(row.projectName());
    }

    private void ensureMapped(TicketImport ticketImport) {
        if (ticketImport.getStatus() == TicketImportStatus.UPLOADED) {
            throw new BadRequestException("Column mapping is required before preview");
        }
    }

    private void validateTitle(String title, List<String> errors) {
        if (isBlank(title)) {
            errors.add("Title is required");
            return;
        }
        if (title.length() < TITLE_MIN) {
            errors.add("Title must be at least %d characters".formatted(TITLE_MIN));
        }
        if (title.length() > TITLE_MAX) {
            errors.add("Title must be at most %d characters".formatted(TITLE_MAX));
        }
    }

    private void validateDescription(String description, List<String> errors) {
        if (isBlank(description)) {
            errors.add("Description is required");
            return;
        }
        if (description.length() < DESCRIPTION_MIN) {
            errors.add("Description must be at least %d characters".formatted(DESCRIPTION_MIN));
        }
        if (description.length() > DESCRIPTION_MAX) {
            errors.add("Description must be at most %d characters".formatted(DESCRIPTION_MAX));
        }
    }

    private Optional<WorkflowStatus> resolveStatusInProject(Project project, String statusName) {
        return project.getWorkflow()
                      .getStatuses()
                      .stream()
                      .filter(s -> s.getName().equalsIgnoreCase(statusName.trim()))
                      .findFirst();
    }

    private boolean hasDirectTransition(Project project, WorkflowStatus from, WorkflowStatus to) {
        return project.getWorkflow()
                      .getTransitions()
                      .stream()
                      .anyMatch(t -> Objects.equals(t.getFrom().getId(), from.getId())
                              && Objects.equals(t.getTo().getId(), to.getId()));
    }

    private TicketImport requireImport(Long projectId, long importId) {
        if (projectId != null) {
            return importRepository.findByIdAndProjectId(importId, projectId)
                                   .orElseThrow(() -> new NotFoundException("Import does not found! importId=%d".formatted(importId)));
        }
        return importRepository.findGlobalById(importId)
                               .orElseThrow(() -> new NotFoundException("Import does not found! importId=%d".formatted(importId)));
    }

    private Project requireProject(long projectId) {
        return projectRepository.findById(projectId)
                                .orElseThrow(() -> new NotFoundException("Project does not found! projectId=%d".formatted(projectId)));
    }

    private static String cellValue(Map<String, String> values, String column) {
        if (isBlank(column)) {
            return "";
        }
        return values.getOrDefault(column, "").trim();
    }

    private static String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static TicketPriority parsePriority(String raw) {
        if (isBlank(raw)) {
            return TicketPriority.MEDIUM;
        }
        try {
            return TicketPriority.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
