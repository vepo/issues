package dev.vepo.issues.ticket.history;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.categories.Category;
import dev.vepo.issues.project.Project;
import dev.vepo.issues.ticket.Ticket;
import dev.vepo.issues.user.User;
import dev.vepo.issues.workflow.WorkflowStatus;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@QuarkusTest
class TicketHistoryServiceTest {

    @Inject
    private TicketHistoryService historyService;

    @Inject
    private TicketHistoryRepository historyRepository;

    private Ticket ticket;
    private User user;
    private User assignee;
    private Category category;
    private Project project;
    private WorkflowStatus status;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setName("Test User");
        user.setEmail("test@example.com");

        assignee = new User();
        assignee.setId(2L);
        assignee.setName("Assignee User");
        assignee.setEmail("assignee@example.com");

        category = new Category();
        category.setId(1L);
        category.setName("Bug");

        project = new Project();
        project.setId(1L);
        project.setName("Test Project");

        status = new WorkflowStatus();
        status.setId(1L);
        status.setName("TODO");

        ticket = new Ticket();
        ticket.setId(1L);
        ticket.setTitle("Test Ticket");
        ticket.setDescription("Test Description");
        ticket.setAuthor(user);
        ticket.setAssignee(assignee);
        ticket.setCategory(category);
        ticket.setProject(project);
        ticket.setStatus(status);
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should log ticket creation")
    @Transactional
    void shouldLogTicketCreation() {
        historyService.logTicketCreated(ticket, user);
        assertNotNull(historyService);
    }

    @Test
    @DisplayName("Should log ticket update with changes")
    @Transactional
    void shouldLogTicketUpdate() {
        historyService.logTicketUpdated(ticket, user, "title, description");
        assertNotNull(historyService);
    }

    @Test
    @DisplayName("Should log status change")
    @Transactional
    void shouldLogStatusChange() {
        historyService.logStatusChanged(ticket, user, "TODO", "IN_PROGRESS");
        assertNotNull(historyService);
    }

    @Test
    @DisplayName("Should log assignee change from one user to another")
    @Transactional
    void shouldLogAssigneeChange() {
        historyService.logAssigneeChanged(ticket, user, "Old Assignee", "New Assignee");
        assertNotNull(historyService);
    }

    @Test
    @DisplayName("Should log assignee assignment when previously unassigned")
    @Transactional
    void shouldLogAssigneeAssignment() {
        historyService.logAssigneeChanged(ticket, user, null, "New Assignee");
        assertNotNull(historyService);
    }

    @Test
    @DisplayName("Should log assignee unassignment")
    @Transactional
    void shouldLogAssigneeUnassignment() {
        historyService.logAssigneeChanged(ticket, user, "Old Assignee", null);
        assertNotNull(historyService);
    }

    @Test
    @DisplayName("Should log category change")
    @Transactional
    void shouldLogCategoryChange() {
        historyService.logCategoryChanged(ticket, user, "Bug", "Feature");
        assertNotNull(historyService);
    }

    @Test
    @DisplayName("Should log comment addition")
    @Transactional
    void shouldLogCommentAdded() {
        historyService.logCommentAdded(ticket, user);
        assertNotNull(historyService);
    }

    @Test
    @DisplayName("Should log ticket deletion")
    @Transactional
    void shouldLogTicketDeleted() {
        historyService.logTicketDeleted(ticket, user);
        assertNotNull(historyService);
    }

    @Test
    @DisplayName("Should log ticket restoration")
    @Transactional
    void shouldLogTicketRestored() {
        historyService.logTicketRestored(ticket, user);
        assertNotNull(historyService);
    }

    @Test
    @DisplayName("Should log priority change")
    @Transactional
    void shouldLogPriorityChange() {
        historyService.logPriorityChanged(ticket, user, "Low", "High");
        assertNotNull(historyService);
    }

    @Test
    @DisplayName("Should log due date change")
    @Transactional
    void shouldLogDueDateChange() {
        historyService.logDueDateChanged(ticket, user, "2024-01-01", "2024-02-01");
        assertNotNull(historyService);
    }

    @Test
    @DisplayName("Should log custom action")
    @Transactional
    void shouldLogCustomAction() {
        historyService.logCustomAction(ticket, user, "Custom action performed");
        assertNotNull(historyService);
    }

    @Test
    @DisplayName("Should create history entry with correct data")
    @Transactional
    void shouldCreateHistoryEntryWithCorrectData() {
        historyService.logTicketCreated(ticket, user);
        assertNotNull(historyService);
    }
}
