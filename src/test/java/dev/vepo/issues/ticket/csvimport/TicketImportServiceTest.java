package dev.vepo.issues.ticket.csvimport;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.ticket.TicketPriority;
import dev.vepo.issues.ticket.TicketRepository;
import dev.vepo.issues.ticket.TicketTestFixtures;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@QuarkusTest
class TicketImportServiceTest {

    @Inject
    TicketImportService ticketImportService;

    @Inject
    TicketRepository ticketRepository;

    @Inject
    TicketImportRowRepository importRowRepository;

    @Inject
    TicketImportRepository importRepository;

    private TicketTestFixtures fixtures;
    private ColumnMapping mapping;

    @BeforeEach
    void setUp() {
        fixtures = TicketTestFixtures.create();
        mapping = new ColumnMapping("Title", "Description", "Category", "Priority", "Assignee", "Status", null, Map.of());
    }

    @Test
    @DisplayName("Should parse and store CSV upload in database")
    void shouldParseAndStoreCsvUploadInDatabase() {
        var csv = """
                  Title,Description,Category,Priority
                  Import ticket title,Import ticket description here,%s,HIGH
                  """.formatted(fixtures.bug().getName());

        var upload = ticketImportService.upload(fixtures.project().id(),
                                                "tickets.csv",
                                                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)),
                                                "project-manager");

        assertThat(upload.id()).isPositive();
        assertThat(upload.headers()).containsExactly("Title", "Description", "Category", "Priority");
        assertThat(upload.rowCount()).isEqualTo(1);

        var stored = importRepository.findById(upload.id()).orElseThrow();
        assertThat(stored.getRows()).hasSize(1);
        assertThat(stored.getStatus()).isEqualTo(TicketImportStatus.UPLOADED);
    }

    @Test
    @DisplayName("Should map stored CSV row using column mapping")
    void shouldMapCsvRowUsingColumnMapping() {
        var importId = uploadSampleCsv();
        ticketImportService.applyMapping(fixtures.project().id(), importId, mapping);

        var row = importRowRepository.findByImportId(importId).getFirst();
        assertThat(row.getTitle()).isEqualTo("Import ticket title");
        assertThat(row.getCategoryName()).isEqualTo(fixtures.bug().getName());
        assertThat(row.getPriority()).isEqualTo(TicketPriority.HIGH);
    }

    @Test
    @DisplayName("Should fail row when category name is unknown")
    void shouldFailRowWhenCategoryNameUnknown() {
        var importId = uploadCsvWithCategory("UnknownCategory" + UUID.randomUUID());
        ticketImportService.applyMapping(fixtures.project().id(), importId, mapping);

        var preview = ticketImportService.preview(fixtures.project().id(), importId);

        assertThat(preview.invalidCount()).isEqualTo(1);
        assertThat(preview.rows().getFirst().errors()).anyMatch(e -> e.contains("Unknown category"));
    }

    @Test
    @DisplayName("Should fail row when assignee email is unknown")
    void shouldFailRowWhenAssigneeEmailUnknown() {
        var csv = """
                  Title,Description,Category,Assignee
                  Valid title here,Valid description text,%s,missing-%s@issues.vepo.dev
                  """.formatted(fixtures.bug().getName(), UUID.randomUUID());
        var importId = uploadCsv(csv);
        ticketImportService.applyMapping(fixtures.project().id(), importId, mapping);

        var preview = ticketImportService.preview(fixtures.project().id(), importId);

        assertThat(preview.invalidCount()).isEqualTo(1);
        assertThat(preview.rows().getFirst().errors()).anyMatch(e -> e.contains("Unknown assignee email"));
    }

    @Test
    @DisplayName("Should fail row when status is not in project workflow")
    void shouldFailRowWhenStatusNotInProjectWorkflow() {
        var csv = """
                  Title,Description,Category,Status
                  Valid title here,Valid description text,%s,NON_EXISTENT_STATUS
                  """.formatted(fixtures.bug().getName());
        var importId = uploadCsv(csv);
        ticketImportService.applyMapping(fixtures.project().id(), importId, mapping);

        var preview = ticketImportService.preview(fixtures.project().id(), importId);

        assertThat(preview.invalidCount()).isEqualTo(1);
        assertThat(preview.rows().getFirst().errors()).anyMatch(e -> e.contains("Status not in project workflow"));
    }

    @Test
    @DisplayName("Should fail row when there is no direct transition from start to status")
    void shouldFailRowWhenNoDirectTransitionFromStartToStatus() {
        var csv = """
                  Title,Description,Category,Status
                  Valid title here,Valid description text,%s,Done
                  """.formatted(fixtures.bug().getName());
        var importId = uploadCsv(csv);
        ticketImportService.applyMapping(fixtures.project().id(), importId, mapping);

        var preview = ticketImportService.preview(fixtures.project().id(), importId);

        assertThat(preview.invalidCount()).isEqualTo(1);
        assertThat(preview.rows().getFirst().errors()).anyMatch(e -> e.contains("No direct transition from start"));
    }

    @Test
    @DisplayName("Should default priority to medium when blank")
    void shouldDefaultPriorityToMediumWhenBlank() {
        var importId = uploadCsvWithCategory(fixtures.bug().getName());
        ticketImportService.applyMapping(fixtures.project().id(),
                                         importId,
                                         new ColumnMapping("Title", "Description", "Category", null, null, null, null, Map.of()));

        var row = importRowRepository.findByImportId(importId).getFirst();
        assertThat(row.getPriority()).isEqualTo(TicketPriority.MEDIUM);
    }

    @Test
    @DisplayName("Should import valid rows and skip invalid rows on partial import")
    @Transactional
    void shouldImportValidRowsAndSkipInvalidOnPartialImport() {
        var beforeCount = ticketRepository.findByProjectId(fixtures.project().id()).count();
        var csv = """
                  Title,Description,Category
                  Valid import title,Valid import description,%s
                  Another valid title,Another valid description,%s
                  """.formatted(fixtures.bug().getName(), "MissingCategory" + UUID.randomUUID());
        var importId = uploadCsv(csv);
        ticketImportService.applyMapping(fixtures.project().id(),
                                         importId,
                                         new ColumnMapping("Title", "Description", "Category", null, null, null, null, Map.of()));

        var response = ticketImportService.execute(fixtures.project().id(), importId, "project-manager");

        assertThat(response.created()).hasSize(1);
        assertThat(response.errors()).hasSize(1);
        assertThat(ticketRepository.findByProjectId(fixtures.project().id()).count()).isEqualTo(beforeCount + 1);
    }

    @Test
    @DisplayName("Should require project column mapping on global import")
    void shouldRequireProjectColumnOnGlobalImport() {
        var importId = uploadGlobalCsv("""
                                       Title,Description,Category
                                       Valid title here,Valid description text,%s
                                       """.formatted(fixtures.bug().getName()));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> ticketImportService.applyMapping(null,
                                                                                                  importId,
                                                                                                  new ColumnMapping("Title",
                                                                                                                    "Description",
                                                                                                                    "Category",
                                                                                                                    null,
                                                                                                                    null,
                                                                                                                    null,
                                                                                                                    null,
                                                                                                                    Map.of())))
                                       .isInstanceOf(jakarta.ws.rs.BadRequestException.class);
    }

    @Test
    @DisplayName("Should resolve project per row on global import")
    @Transactional
    void shouldResolveProjectPerRowOnGlobalImport() {
        var beforeCount = ticketRepository.findByProjectId(fixtures.project().id()).count();
        var csv = """
                  Project,Title,Description,Category
                  %s,Global import title,Global import description,%s
                  """.formatted(fixtures.project().name(), fixtures.bug().getName());
        var importId = uploadGlobalCsv(csv);
        var globalMapping = new ColumnMapping("Title", "Description", "Category", null, null, null, "Project", Map.of());
        ticketImportService.applyMapping(null, importId, globalMapping);

        var response = ticketImportService.execute(null, importId, "project-manager");

        assertThat(response.created()).hasSize(1);
        assertThat(response.errors()).isEmpty();
        assertThat(ticketRepository.findByProjectId(fixtures.project().id()).count()).isEqualTo(beforeCount + 1);
    }

    @Test
    @DisplayName("Should fail global import row when project name is unknown")
    void shouldFailGlobalImportRowWhenProjectUnknown() {
        var importId = uploadGlobalCsv("""
                                       Project,Title,Description,Category
                                       Unknown Project %s,Valid title here,Valid description text,%s
                                       """.formatted(UUID.randomUUID(), fixtures.bug().getName()));
        ticketImportService.applyMapping(null,
                                         importId,
                                         new ColumnMapping("Title", "Description", "Category", null, null, null, "Project", Map.of()));

        var preview = ticketImportService.preview(null, importId);

        assertThat(preview.invalidCount()).isEqualTo(1);
        assertThat(preview.rows().getFirst().errors()).anyMatch(e -> e.contains("Unknown project"));
    }

    @Test
    @DisplayName("Should revalidate row after correcting unknown project")
    void shouldRevalidateRowAfterCorrectingUnknownProject() {
        var importId = uploadGlobalCsv("""
                                       Project,Title,Description,Category
                                       Unknown Project %s,Valid title here,Valid description text,%s
                                       """.formatted(UUID.randomUUID(), fixtures.bug().getName()));
        ticketImportService.applyMapping(null,
                                         importId,
                                         new ColumnMapping("Title", "Description", "Category", null, null, null, "Project", Map.of()));
        var preview = ticketImportService.preview(null, importId);
        var row = preview.rows().getFirst();

        var corrected = ticketImportService.correctRow(null,
                                                       importId,
                                                       row.rowId(),
                                                       new CorrectImportRowRequest(fixtures.project().name(), null, null));

        assertThat(corrected.valid()).isTrue();
        assertThat(corrected.preview().projectName()).isEqualTo(fixtures.project().name());
    }

    @Test
    @DisplayName("Should revalidate row after correcting unknown status")
    void shouldRevalidateRowAfterCorrectingUnknownStatus() {
        var importId = uploadCsv("""
                                 Title,Description,Category,Status
                                 Valid title here,Valid description text,%s,NON_EXISTENT_STATUS
                                 """.formatted(fixtures.bug().getName()));
        ticketImportService.applyMapping(fixtures.project().id(), importId, mapping);
        var preview = ticketImportService.preview(fixtures.project().id(), importId);
        var row = preview.rows().getFirst();

        var corrected = ticketImportService.correctRow(fixtures.project().id(),
                                                       importId,
                                                       row.rowId(),
                                                       new CorrectImportRowRequest(null, "", null));

        assertThat(corrected.valid()).isTrue();
        assertThat(corrected.preview().statusName()).isNull();
    }

    @Test
    @DisplayName("Should revalidate row after correcting unknown assignee")
    void shouldRevalidateRowAfterCorrectingUnknownAssignee() {
        var importId = uploadCsv("""
                                 Title,Description,Category,Assignee
                                 Valid title here,Valid description text,%s,missing-%s@issues.vepo.dev
                                 """.formatted(fixtures.bug().getName(), UUID.randomUUID()));
        ticketImportService.applyMapping(fixtures.project().id(), importId, mapping);

        var preview = ticketImportService.preview(fixtures.project().id(), importId);
        var row = preview.rows().getFirst();

        var corrected = ticketImportService.correctRow(fixtures.project().id(),
                                                       importId,
                                                       row.rowId(),
                                                       new CorrectImportRowRequest(null, null, "user@issues.vepo.dev"));

        assertThat(corrected.valid()).isTrue();
        assertThat(corrected.preview().assigneeEmail()).isEqualTo("user@issues.vepo.dev");
    }

    private long uploadSampleCsv() {
        return uploadCsvWithCategory(fixtures.bug().getName());
    }

    private long uploadCsvWithCategory(String categoryName) {
        var csv = """
                  Title,Description,Category,Priority,Assignee,Status
                  Import ticket title,Import ticket description here,%s,HIGH,user@issues.vepo.dev,In Progress
                  """.formatted(categoryName);
        return uploadCsv(csv);
    }

    private long uploadCsv(String csv) {
        return ticketImportService.upload(fixtures.project().id(),
                                          "tickets.csv",
                                          new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)),
                                          "project-manager")
                                  .id();
    }

    private long uploadGlobalCsv(String csv) {
        return ticketImportService.upload(null,
                                          "tickets.csv",
                                          new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)),
                                          "project-manager")
                                  .id();
    }
}
