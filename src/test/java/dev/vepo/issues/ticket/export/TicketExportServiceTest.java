package dev.vepo.issues.ticket.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.categories.Category;
import dev.vepo.issues.customfield.CustomFieldService;
import dev.vepo.issues.customfield.CustomFieldType;
import dev.vepo.issues.customfield.CustomFieldValueResponse;
import dev.vepo.issues.phase.Phase;
import dev.vepo.issues.phase.PhaseStatus;
import dev.vepo.issues.phase.Version;
import dev.vepo.issues.project.Project;
import dev.vepo.issues.project.ProjectAccessService;
import dev.vepo.issues.ticket.Ticket;
import dev.vepo.issues.ticket.TicketPriority;
import dev.vepo.issues.ticket.TicketType;
import dev.vepo.issues.user.Role;
import dev.vepo.issues.user.User;
import dev.vepo.issues.workflow.Workflow;
import dev.vepo.issues.workflow.WorkflowStatus;
import jakarta.ws.rs.BadRequestException;

class TicketExportServiceTest {

    private static final Set<Long> READABLE_PROJECT_IDS = Set.of(11L, 12L);

    private RecordingTicketExportSearchService repository;
    private RecordingProjectAccessService projectAccessService;
    private RecordingCustomFieldService customFieldService;
    private TicketExportService service;
    private User requestingUser;

    @BeforeEach
    void setup() {
        repository = new RecordingTicketExportSearchService();
        projectAccessService = new RecordingProjectAccessService(READABLE_PROJECT_IDS);
        customFieldService = new RecordingCustomFieldService();
        service = new TicketExportService(repository, projectAccessService, customFieldService);
        requestingUser = new User("reader", "Readable User", "reader@example.test", "encoded", Set.of(Role.USER));
        requestingUser.setId(7L);
    }

    @Test
    void shouldRejectMixedAndIncompleteSimpleSearchSourceFields() {
        var mixed = request(ExportSource.SIMPLE_SEARCH, "release", 3L, "priority = HIGH", null);
        var incompleteAdvanced = request(ExportSource.ADVANCED_QUERY, null, null, null, null);

        assertThatThrownBy(() -> service.prepare(mixed, requestingUser))
                                                                        .isInstanceOf(BadRequestException.class)
                                                                        .hasMessageContaining("source");
        assertThatThrownBy(() -> service.prepare(incompleteAdvanced, requestingUser))
                                                                                     .isInstanceOf(BadRequestException.class)
                                                                                     .hasMessageContaining("query");
        assertThat(repository.searches).isEmpty();
    }

    @Test
    void shouldRejectFieldsThatDoNotBelongToSavedQuerySource() {
        var mixed = request(ExportSource.SAVED_QUERY, "browser rows", null, null, "release-readiness");
        var incomplete = request(ExportSource.SAVED_QUERY, null, null, null, null);

        assertThatThrownBy(() -> service.prepare(mixed, requestingUser))
                                                                        .isInstanceOf(BadRequestException.class)
                                                                        .hasMessageContaining("source");
        assertThatThrownBy(() -> service.prepare(incomplete, requestingUser))
                                                                             .isInstanceOf(BadRequestException.class)
                                                                             .hasMessageContaining("savedQuerySlug");
    }

    @Test
    void shouldDispatchSimpleAdvancedAndSavedSourcesWithReadableProjectIds() {
        repository.results = List.of(ticket(1L, "ISS-1", false));

        service.prepare(request(ExportSource.SIMPLE_SEARCH, "release", 3L, null, null), requestingUser);
        service.prepare(request(ExportSource.ADVANCED_QUERY, null, null, "priority = HIGH ORDER BY updated DESC", null),
                        requestingUser);
        service.prepare(request(ExportSource.SAVED_QUERY, null, null, null, "release-readiness"), requestingUser);

        assertThat(repository.searches)
                                       .extracting(search -> search.criteria().getClass().getSimpleName())
                                       .containsExactly("SimpleTicketExportCriteria",
                                                        "AdvancedTicketExportCriteria",
                                                        "SavedTicketExportCriteria");
        assertThat(repository.searches).allSatisfy(search -> {
            assertThat(search.readableProjectIds()).isEqualTo(READABLE_PROJECT_IDS);
            assertThat(search.includeDeleted()).isFalse();
            assertThat(search.limit()).isEqualTo(10_001);
        });
        assertThat(repository.searches.get(0).criteria()).isEqualTo(new SimpleTicketExportCriteria("release", 3L));
        assertThat(repository.searches.get(1).criteria())
                                                         .isEqualTo(new AdvancedTicketExportCriteria("priority = HIGH ORDER BY updated DESC"));
        assertThat(repository.searches.get(2).criteria()).isEqualTo(new SavedTicketExportCriteria("release-readiness"));
        assertThat(projectAccessService.requestedUsers).containsExactly(requestingUser, requestingUser, requestingUser);
    }

    @Test
    void shouldPreserveExplicitAdvancedQueryOrdering() {
        repository.results = List.of(ticket(2L, "ISS-2", false), ticket(1L, "ISS-1", false));
        repository.explicitOrder = true;

        var rows = service.prepare(request(ExportSource.ADVANCED_QUERY,
                                           null,
                                           null,
                                           "project = \"Issues\" ORDER BY updated DESC",
                                           null),
                                   requestingUser);

        assertThat(rows).extracting(TicketExportRow::identifier).containsExactly("ISS-2", "ISS-1");
    }

    @Test
    void shouldPreserveExplicitSavedQueryOrderingResolvedByRepository() {
        repository.results = List.of(ticket(2L, "ISS-2", false), ticket(1L, "ISS-1", false));
        repository.explicitOrder = true;

        var rows = service.prepare(request(ExportSource.SAVED_QUERY, null, null, null, "recently-updated"),
                                   requestingUser);

        assertThat(rows).extracting(TicketExportRow::identifier).containsExactly("ISS-2", "ISS-1");
    }

    @Test
    void shouldOrderSourcesWithoutExplicitOrderingByIdentifier() {
        repository.results = List.of(ticket(12L, "ISS-12", false),
                                     ticket(2L, "ISS-2", false),
                                     ticket(1L, "ISS-1", false));

        var simpleRows = service.prepare(request(ExportSource.SIMPLE_SEARCH, null, null, null, null), requestingUser);
        var savedRows = service.prepare(request(ExportSource.SAVED_QUERY, null, null, null, "my-query"), requestingUser);

        assertThat(simpleRows).extracting(TicketExportRow::identifier).containsExactly("ISS-1", "ISS-12", "ISS-2");
        assertThat(savedRows).extracting(TicketExportRow::identifier).containsExactly("ISS-1", "ISS-12", "ISS-2");
    }

    @Test
    void shouldExcludeDeletedTicketsEvenWhenADataSourceReturnsThem() {
        repository.results = List.of(ticket(1L, "ISS-1", false), ticket(2L, "ISS-2", true));

        var rows = service.prepare(request(ExportSource.SIMPLE_SEARCH, null, null, null, null), requestingUser);

        assertThat(rows).extracting(TicketExportRow::identifier).containsExactly("ISS-1");
    }

    @Test
    void shouldQueryOneBeyondLimitAndRejectMoreThanTenThousandTickets() {
        repository.results = new ArrayList<>(10_001);
        var ticket = ticket(1L, "ISS-1", false);
        for (var index = 0; index < 10_001; index++) {
            repository.results.add(ticket);
        }
        var request = request(ExportSource.SIMPLE_SEARCH, null, null, null, null);

        assertThatThrownBy(() -> service.prepare(request, requestingUser))
                                                                          .isInstanceOf(TicketExportLimitExceededException.class)
                                                                          .hasMessageContaining("10,000");
        assertThat(repository.searches).singleElement().satisfies(search -> assertThat(search.limit()).isEqualTo(10_001));
        assertThat(customFieldService.requestedTicketIds).isEmpty();
    }

    @Test
    void shouldBuildDedicatedRowsWithStableFieldsDisplayNamesAndPlainText() {
        var ticket = ticket(42L, "ISS-42", false);
        ticket.setDescription("<p>First <strong>line</strong></p><p>Second &amp; final</p>");
        ticket.setPriority(TicketPriority.HIGH);
        ticket.setTicketType(TicketType.STORY);
        ticket.setStoryPoints(8);
        ticket.setDueDate(LocalDate.of(2026, Month.JULY, 31));
        ticket.setCreatedAt(LocalDateTime.of(2026, Month.JULY, 16, 10, 20, 30));
        ticket.setUpdatedAt(LocalDateTime.of(2026, Month.JULY, 17, 11, 21, 31));
        var phase = new Phase(ticket.getProject(), "Delivery", "Ship", PhaseStatus.ACTIVE, LocalDateTime.now());
        phase.setId(61L);
        ticket.setPhase(phase);
        var observed = new Version(ticket.getProject(), "1.0", null);
        observed.setId(71L);
        ticket.setObservedVersion(observed);
        var target = new Version(ticket.getProject(), "1.1", null);
        target.setId(72L);
        ticket.setTargetVersion(target);
        repository.results = List.of(ticket);

        var row = service.prepare(request(ExportSource.SIMPLE_SEARCH, null, null, null, null), requestingUser).getFirst();

        assertThat(row.identifier()).isEqualTo("ISS-42");
        assertThat(row.title()).isEqualTo("Ticket ISS-42");
        assertThat(row.description()).isEqualTo("First line\nSecond & final");
        assertThat(row.projectKey()).isEqualTo("ISS");
        assertThat(row.projectName()).isEqualTo("Issues");
        assertThat(row.statusCode()).isEqualTo("IN_PROGRESS");
        assertThat(row.statusName()).isEqualTo("In Progress");
        assertThat(row.categoryId()).isEqualTo(31L);
        assertThat(row.categoryName()).isEqualTo("Defect");
        assertThat(row.priority()).isEqualTo(TicketPriority.HIGH);
        assertThat(row.type()).isEqualTo(TicketType.STORY);
        assertThat(row.authorEmail()).isEqualTo("author@example.test");
        assertThat(row.authorName()).isEqualTo("Ticket Author");
        assertThat(row.assigneeEmail()).isEqualTo("assignee@example.test");
        assertThat(row.assigneeName()).isEqualTo("Ticket Assignee");
        assertThat(row.phaseId()).isEqualTo(61L);
        assertThat(row.phaseName()).isEqualTo("Delivery");
        assertThat(row.observedVersionId()).isEqualTo(71L);
        assertThat(row.observedVersionName()).isEqualTo("1.0");
        assertThat(row.targetVersionId()).isEqualTo(72L);
        assertThat(row.targetVersionName()).isEqualTo("1.1");
        assertThat(row.storyPoints()).isEqualTo(8);
        assertThat(row.dueDate()).isEqualTo(LocalDate.of(2026, Month.JULY, 31));
        assertThat(row.createdAt()).isEqualTo(LocalDateTime.of(2026, Month.JULY, 16, 10, 20, 30));
        assertThat(row.updatedAt()).isEqualTo(LocalDateTime.of(2026, Month.JULY, 17, 11, 21, 31));
    }

    @Test
    void shouldBatchTypedCustomFieldsForAllExportRows() {
        repository.results = List.of(ticket(2L, "ISS-2", false), ticket(1L, "ISS-1", false));
        customFieldService.values = Map.of(1L,
                                           List.of(new CustomFieldValueResponse("customer", CustomFieldType.STRING, "Acme", false, false),
                                                   new CustomFieldValueResponse("approved", CustomFieldType.BOOLEAN, true, false, false)),
                                           2L,
                                           List.of(new CustomFieldValueResponse("estimate", CustomFieldType.INTEGER, 13, false, false),
                                                   new CustomFieldValueResponse("notes",
                                                                                CustomFieldType.TEXT,
                                                                                "<p>Ready <em>now</em></p>",
                                                                                false,
                                                                                false)));

        var rows = service.prepare(request(ExportSource.SIMPLE_SEARCH, null, null, null, null), requestingUser);

        assertThat(customFieldService.requestedTicketIds).containsExactly(Set.of(1L, 2L));
        assertThat(rows.get(0).customFields()).containsEntry("approved", true).containsEntry("customer", "Acme");
        assertThat(rows.get(0).customFields().keySet()).containsExactly("approved", "customer");
        assertThat(rows.get(1).customFields()).containsEntry("estimate", 13).containsEntry("notes", "Ready now");
        assertThat(rows.get(1).customFields().keySet()).containsExactly("estimate", "notes");
        assertThat(rows.get(0).customFields().get("approved")).isInstanceOf(Boolean.class);
        assertThat(rows.get(1).customFields().get("estimate")).isInstanceOf(Integer.class);
    }

    @Test
    void shouldReturnEmptyRowsWithoutLoadingCustomFields() {
        repository.results = List.of();

        var rows = service.prepare(request(ExportSource.SIMPLE_SEARCH, "absent", null, null, null), requestingUser);

        assertThat(rows).isEmpty();
        assertThat(customFieldService.requestedTicketIds).isEmpty();
    }

    private static ExportTicketsRequest request(ExportSource source,
                                                String term,
                                                Long statusId,
                                                String query,
                                                String savedQuerySlug) {
        return new ExportTicketsRequest(ExportFormat.JSON, source, term, statusId, query, savedQuerySlug);
    }

    private static Ticket ticket(long id, String identifier, boolean deleted) {
        var author = new User("author", "Ticket Author", "author@example.test", "encoded", Set.of(Role.USER));
        author.setId(21L);
        var assignee = new User("assignee", "Ticket Assignee", "assignee@example.test", "encoded", Set.of(Role.USER));
        assignee.setId(22L);
        var status = new WorkflowStatus("In Progress");
        status.setId(41L);
        var workflow = new Workflow("Delivery", List.of(status), status, List.of());
        workflow.setId(51L);
        var project = new Project("ISS", "Issues", "Tracker", workflow, author);
        project.setId(11L);
        var category = new Category("Defect");
        category.setId(31L);
        var ticket = new Ticket(identifier,
                                "Ticket %s".formatted(identifier),
                                "Description",
                                category,
                                author,
                                assignee,
                                project,
                                status);
        ticket.setId(id);
        ticket.setDeleted(deleted);
        return ticket;
    }

    private record SearchCall(TicketExportCriteria criteria,
                              Set<Long> readableProjectIds,
                              int limit,
                              boolean includeDeleted) {}

    private static final class RecordingTicketExportSearchService extends TicketExportSearchService {
        private final List<SearchCall> searches = new ArrayList<>();
        private List<Ticket> results = List.of();
        private boolean explicitOrder;

        private RecordingTicketExportSearchService() {
            super(null, null, null);
        }

        @Override
        public TicketExportSearchResult search(TicketExportCriteria criteria,
                                               Set<Long> readableProjectIds,
                                               int limit,
                                               boolean includeDeleted,
                                               User requestingUser) {
            searches.add(new SearchCall(criteria, readableProjectIds, limit, includeDeleted));
            return new TicketExportSearchResult(results, explicitOrder);
        }
    }

    private static final class RecordingProjectAccessService extends ProjectAccessService {
        private final Set<Long> readableProjectIds;
        private final List<User> requestedUsers = new ArrayList<>();

        private RecordingProjectAccessService(Set<Long> readableProjectIds) {
            super(null, null, null);
            this.readableProjectIds = readableProjectIds;
        }

        @Override
        public Set<Long> readableProjectIds(User user) {
            requestedUsers.add(user);
            return readableProjectIds;
        }
    }

    private static final class RecordingCustomFieldService extends CustomFieldService {
        private final List<Set<Long>> requestedTicketIds = new ArrayList<>();
        private Map<Long, List<CustomFieldValueResponse>> values = Map.of();

        private RecordingCustomFieldService() {
            super(null, null, null, null);
        }

        @Override
        public Map<Long, List<CustomFieldValueResponse>> readValuesByTicketIds(Collection<Long> ticketIds) {
            requestedTicketIds.add(Set.copyOf(ticketIds));
            return values;
        }
    }
}
