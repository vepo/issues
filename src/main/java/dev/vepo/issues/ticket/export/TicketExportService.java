package dev.vepo.issues.ticket.export;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import dev.vepo.issues.customfield.CustomFieldService;
import dev.vepo.issues.customfield.CustomFieldType;
import dev.vepo.issues.customfield.CustomFieldValueResponse;
import dev.vepo.issues.project.ProjectAccessService;
import dev.vepo.issues.ticket.Ticket;
import dev.vepo.issues.user.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;

@ApplicationScoped
public class TicketExportService {

    static final int MAX_TICKETS = 10_000;
    private static final int SEARCH_LIMIT = MAX_TICKETS + 1;

    private final TicketExportSearchService searchService;
    private final ProjectAccessService projectAccessService;
    private final CustomFieldService customFieldService;

    @Inject
    public TicketExportService(TicketExportSearchService searchService,
                               ProjectAccessService projectAccessService,
                               CustomFieldService customFieldService) {
        this.searchService = searchService;
        this.projectAccessService = projectAccessService;
        this.customFieldService = customFieldService;
    }

    @Transactional
    public List<TicketExportRow> prepare(ExportTicketsRequest request, User requestingUser) {
        var criteria = validateAndLoadCriteria(request);
        var readableProjectIds = projectAccessService.readableProjectIds(requestingUser);
        var searchResult = searchService.search(criteria, readableProjectIds, SEARCH_LIMIT, false, requestingUser);
        var tickets = searchResult.tickets()
                                  .stream()
                                  .filter(ticket -> !ticket.isDeleted())
                                  .toList();
        if (tickets.size() > MAX_TICKETS) {
            throw new TicketExportLimitExceededException();
        }
        if (tickets.isEmpty()) {
            return List.of();
        }
        if (!searchResult.explicitOrder()) {
            tickets = tickets.stream()
                             .sorted(Comparator.comparing(Ticket::getIdentifier))
                             .toList();
        }
        var valuesByTicket = customFieldService.readValuesByTicketIds(tickets.stream()
                                                                             .map(Ticket::getId)
                                                                             .collect(Collectors.toSet()));
        return tickets.stream()
                      .map(ticket -> toRow(ticket, valuesByTicket.getOrDefault(ticket.getId(), List.of())))
                      .toList();
    }

    private TicketExportCriteria validateAndLoadCriteria(ExportTicketsRequest request) {
        if (request == null || request.format() == null || request.source() == null) {
            throw new BadRequestException("Export format and source are required");
        }
        return switch (request.source()) {
            case SIMPLE_SEARCH -> {
                if (hasText(request.query()) || hasText(request.savedQuerySlug())) {
                    throw new BadRequestException("Fields do not match the selected source");
                }
                yield new SimpleTicketExportCriteria(trimToNull(request.term()), request.statusId());
            }
            case ADVANCED_QUERY -> {
                if (!hasText(request.query())) {
                    throw new BadRequestException("Advanced export query is required");
                }
                if (hasText(request.term()) || request.statusId() != null || hasText(request.savedQuerySlug())) {
                    throw new BadRequestException("Fields do not match the selected source");
                }
                yield new AdvancedTicketExportCriteria(request.query().trim());
            }
            case SAVED_QUERY -> {
                if (!hasText(request.savedQuerySlug())) {
                    throw new BadRequestException("savedQuerySlug is required");
                }
                if (hasText(request.term()) || request.statusId() != null || hasText(request.query())) {
                    throw new BadRequestException("Fields do not match the selected source");
                }
                yield new SavedTicketExportCriteria(request.savedQuerySlug().trim());
            }
        };
    }

    private TicketExportRow toRow(Ticket ticket, List<CustomFieldValueResponse> customFieldValues) {
        var project = ticket.getProject();
        var status = ticket.getStatus();
        var category = ticket.getCategory();
        var author = ticket.getAuthor();
        var assignee = ticket.getAssignee();
        var phase = ticket.getPhase();
        var observedVersion = ticket.getObservedVersion();
        var targetVersion = ticket.getTargetVersion();
        return new TicketExportRow(ticket.getIdentifier(),
                                   ticket.getTitle(),
                                   TicketExportValueConverter.toJsonPlainText(ticket.getDescription()),
                                   project.getPrefix(),
                                   project.getName(),
                                   stableCode(status.getName()),
                                   status.getName(),
                                   category == null ? null : category.getId(),
                                   category == null ? null : category.getName(),
                                   ticket.getPriority(),
                                   ticket.getTicketType(),
                                   author == null ? null : author.getEmail(),
                                   author == null ? null : author.getName(),
                                   assignee == null ? null : assignee.getEmail(),
                                   assignee == null ? null : assignee.getName(),
                                   phase == null ? null : phase.getId(),
                                   phase == null ? null : phase.getName(),
                                   observedVersion == null ? null : observedVersion.getId(),
                                   observedVersion == null ? null : observedVersion.getLabel(),
                                   targetVersion == null ? null : targetVersion.getId(),
                                   targetVersion == null ? null : targetVersion.getLabel(),
                                   ticket.getStoryPoints(),
                                   ticket.getDueDate(),
                                   ticket.getCreatedAt(),
                                   ticket.getUpdatedAt(),
                                   stableCustomFields(customFieldValues));
    }

    private Map<String, Object> stableCustomFields(List<CustomFieldValueResponse> values) {
        var sorted = new TreeMap<String, Object>();
        for (var value : values) {
            if (!value.orphan() && value.value() != null) {
                sorted.put(value.key(),
                           value.type() == CustomFieldType.TEXT && value.value() instanceof String text
                                                                                                        ? TicketExportValueConverter.toJsonPlainText(text)
                                                                                                        : value.value());
            }
        }
        return new LinkedHashMap<>(sorted);
    }

    private static String stableCode(String name) {
        var code = name.trim()
                       .toUpperCase(Locale.ROOT)
                       .replaceAll("[^A-Z0-9]+", "_");
        if (code.startsWith("_")) {
            code = code.substring(1);
        }
        if (code.endsWith("_")) {
            code = code.substring(0, code.length() - 1);
        }
        return code;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }
}
