package dev.vepo.issues.ticket.reminders;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import dev.vepo.issues.categories.Category;
import dev.vepo.issues.categories.CategoryRepository;
import dev.vepo.issues.notifications.Notification;
import dev.vepo.issues.notifications.NotificationRepository;
import dev.vepo.issues.notifications.NotificationService;
import dev.vepo.issues.ticket.TicketRepository;
import dev.vepo.issues.ticket.TicketResponse;
import dev.vepo.issues.user.User;
import dev.vepo.issues.user.UserRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class DueDateReminderServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-17T06:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.ofInstant(NOW, ZoneOffset.UTC);

    @Test
    void shouldNotifyAssigneeWhenTicketDueTomorrowTest() {
        var assignee = Given.randomUser();
        var project = Given.simpleProject();
        Given.addProjectMember(project.id(), assignee.getId());
        var ticket = createTicket(project.id());
        setDueDateAndAssignee(ticket.id(), TODAY.plusDays(1), assignee.getId());

        Given.transaction(() -> service().checkAndNotify());

        assertThat(dueDateReminderNotificationsFor(assignee, ticket.id())).hasSize(1);
        assertThat(reminderMarkers(ticket.id())[0]).isNotNull();
    }

    @Test
    void shouldNotifyAssigneeWhenTicketOverdueTest() {
        var assignee = Given.randomUser();
        var project = Given.simpleProject();
        Given.addProjectMember(project.id(), assignee.getId());
        var ticket = createTicket(project.id());
        setDueDateAndAssignee(ticket.id(), TODAY.minusDays(1), assignee.getId());

        Given.transaction(() -> service().checkAndNotify());

        assertThat(dueDateReminderNotificationsFor(assignee, ticket.id())).hasSize(1);
        assertThat(reminderMarkers(ticket.id())[1]).isNotNull();
    }

    @Test
    void shouldExcludeFinishedCanceledDeletedAndUnassignedTicketsTest() {
        var assignee = Given.randomUser();
        var project = Given.simpleProject();
        Given.addProjectMember(project.id(), assignee.getId());

        var finished = createTicket(project.id());
        setDueDateAndAssignee(finished.id(), TODAY.minusDays(1), assignee.getId());
        Given.transaction(() -> Given.inject(TicketRepository.class)
                                     .findById(finished.id())
                                     .orElseThrow()
                                     .setFinishedAt(LocalDateTime.now()));

        var canceled = createTicket(project.id());
        setDueDateAndAssignee(canceled.id(), TODAY.minusDays(1), assignee.getId());
        Given.transaction(() -> Given.inject(TicketRepository.class)
                                     .findById(canceled.id())
                                     .orElseThrow()
                                     .setCanceledAt(LocalDateTime.now()));

        var deleted = createTicket(project.id());
        setDueDateAndAssignee(deleted.id(), TODAY.minusDays(1), assignee.getId());
        Given.transaction(() -> Given.inject(TicketRepository.class).delete(deleted.id()));

        var unassigned = createTicket(project.id());
        Given.transaction(() -> Given.inject(TicketRepository.class)
                                     .findById(unassigned.id())
                                     .orElseThrow()
                                     .setDueDate(TODAY.minusDays(1)));

        Given.transaction(() -> service().checkAndNotify());

        assertThat(dueDateReminderNotificationsFor(assignee, finished.id())).isEmpty();
        assertThat(dueDateReminderNotificationsFor(assignee, canceled.id())).isEmpty();
        assertThat(dueDateReminderNotificationsFor(assignee, deleted.id())).isEmpty();
        assertThat(dueDateReminderNotificationsFor(assignee, unassigned.id())).isEmpty();
    }

    @Test
    void shouldNotDoubleFireAnAlreadySentReminderTest() {
        var assignee = Given.randomUser();
        var project = Given.simpleProject();
        Given.addProjectMember(project.id(), assignee.getId());
        var ticket = createTicket(project.id());
        setDueDateAndAssignee(ticket.id(), TODAY.plusDays(1), assignee.getId());

        Given.transaction(() -> service().checkAndNotify());
        Given.transaction(() -> service().checkAndNotify());

        assertThat(dueDateReminderNotificationsFor(assignee, ticket.id())).hasSize(1);
    }

    @Test
    void shouldResetMarkersWhenDueDateChangesTest() {
        var assignee = Given.randomUser();
        var project = Given.simpleProject();
        Given.addProjectMember(project.id(), assignee.getId());
        var ticket = createTicket(project.id());
        setDueDateAndAssignee(ticket.id(), TODAY.plusDays(1), assignee.getId());
        Given.transaction(() -> service().checkAndNotify());
        assertThat(reminderMarkers(ticket.id())[0]).isNotNull();

        given().header(Given.authenticatedProjectManager())
               .contentType(ContentType.JSON)
               .when()
               .body("""
                     {
                         "title": "%s",
                         "description": "%s",
                         "categoryId": %d,
                         "priority": "%s",
                         "dueDate": "%s"
                     }""".formatted(ticket.title(), ticket.description(), ticket.category(), ticket.priority(),
                                    TODAY.plusDays(5)))
               .post("/api/tickets/" + ticket.id())
               .then()
               .statusCode(200);

        assertThat(reminderMarkers(ticket.id())[0]).isNull();
    }

    @Test
    void shouldResetMarkersWhenAssigneeChangesTest() {
        var firstAssignee = Given.randomUser();
        var secondAssignee = Given.randomUser();
        var project = Given.simpleProject();
        Given.addProjectMember(project.id(), firstAssignee.getId());
        Given.addProjectMember(project.id(), secondAssignee.getId());
        var ticket = createTicket(project.id());
        setDueDateAndAssignee(ticket.id(), TODAY.plusDays(1), firstAssignee.getId());
        Given.transaction(() -> service().checkAndNotify());
        assertThat(reminderMarkers(ticket.id())[0]).isNotNull();

        given().header(Given.authenticatedProjectManager())
               .contentType(ContentType.JSON)
               .when()
               .body("""
                     {
                         "assigneeId": %d
                     }""".formatted(secondAssignee.getId()))
               .patch("/api/tickets/" + ticket.id() + "/assignee")
               .then()
               .statusCode(200);

        assertThat(reminderMarkers(ticket.id())[0]).isNull();
    }

    private DueDateReminderService service() {
        return new DueDateReminderService(Given.inject(TicketRepository.class),
                                          Given.inject(NotificationService.class),
                                          FIXED_CLOCK);
    }

    private TicketResponse createTicket(long projectId) {
        var category = Given.transaction(() -> Given.inject(CategoryRepository.class)
                                                    .save(new Category("Reminder" + UUID.randomUUID(), "blue")));
        return given().contentType(ContentType.JSON)
                      .header(Given.authenticatedProjectManager())
                      .body("""
                            {
                                "title": "Reminder ticket %s",
                                "description": "Seed ticket for reminder tests.",
                                "projectId": %d,
                                "categoryId": %d
                            }
                            """.formatted(UUID.randomUUID(), projectId, category.getId()))
                      .when()
                      .post("/api/tickets")
                      .then()
                      .statusCode(201)
                      .extract()
                      .as(TicketResponse.class);
    }

    private void setDueDateAndAssignee(long ticketId, LocalDate dueDate, long assigneeId) {
        Given.transaction(() -> {
            var ticket = Given.inject(TicketRepository.class).findById(ticketId).orElseThrow();
            ticket.setDueDate(dueDate);
            ticket.setAssignee(Given.inject(UserRepository.class).findById(assigneeId).orElseThrow());
        });
    }

    private LocalDateTime[] reminderMarkers(long ticketId) {
        return Given.transaction(() -> {
            var ticket = Given.inject(TicketRepository.class).findById(ticketId).orElseThrow();
            return new LocalDateTime[] { ticket.getDueSoonReminderSentAt(), ticket.getOverdueReminderSentAt() };
        });
    }

    private List<Notification> dueDateReminderNotificationsFor(User user, long ticketId) {
        return Given.transaction(() -> Given.inject(NotificationRepository.class)
                                            .findAll(user.getUsername())
                                            .filter(n -> "due-date-reminder".equals(n.getType())
                                                    && n.getReffer().getId() == ticketId)
                                            .toList());
    }
}
