package dev.vepo.issues.ticket.history;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.Given;
import dev.vepo.issues.ticket.TicketRepository;
import dev.vepo.issues.ticket.TicketTestFixtures;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class TicketHistoryServiceTest {

    @Inject
    TicketHistoryService historyService;

    @Inject
    TicketHistoryRepository historyRepository;

    @Inject
    TicketRepository ticketRepository;

    private TicketTestFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = TicketTestFixtures.create();
    }

    @Test
    @DisplayName("Should persist structured entry when ticket is created")
    void shouldLogTicketCreation() {
        Given.transaction(() -> {
            var ticket = ticketRepository.findById(fixtures.ticket().id()).orElseThrow();
            var user = Given.user("user@issues.vepo.dev");

            historyService.logTicketCreated(ticket, user);

            var created = historyRepository.findByTicketId(ticket.getId())
                                           .stream()
                                           .filter(e -> e.action == TicketHistoryAction.CREATED)
                                           .findFirst()
                                           .orElseThrow();
            assertEquals(TicketHistoryAction.CREATED, created.action);
            assertNull(created.field);
            assertEquals(user.getId(), created.user.getId());
        });
    }

    @Test
    @DisplayName("Should persist field change with old and new values")
    void shouldLogFieldChange() {
        Given.transaction(() -> {
            var ticket = ticketRepository.findById(fixtures.ticket().id()).orElseThrow();
            var user = Given.user("user@issues.vepo.dev");

            historyService.logFieldChanged(ticket, user, "title", "Old Title", "New Title");

            var entry = historyRepository.findByTicketId(ticket.getId())
                                         .stream()
                                         .filter(e -> e.action == TicketHistoryAction.FIELD_CHANGED)
                                         .findFirst()
                                         .orElseThrow();
            assertEquals("title", entry.field);
            assertEquals("Old Title", entry.oldValue);
            assertEquals("New Title", entry.newValue);
        });
    }

    @Test
    @DisplayName("Should normalize status names when logging status change")
    void shouldLogStatusChangeWithNormalizedNames() {
        Given.transaction(() -> {
            var ticket = ticketRepository.findById(fixtures.ticket().id()).orElseThrow();
            var user = Given.user("user@issues.vepo.dev");

            historyService.logStatusChanged(ticket, user, "TODO", "IN_PROGRESS");

            var entry = historyRepository.findByTicketId(ticket.getId())
                                         .stream()
                                         .filter(e -> e.action == TicketHistoryAction.STATUS_CHANGED)
                                         .findFirst()
                                         .orElseThrow();
            assertEquals("status", entry.field);
            assertEquals("Todo", entry.oldValue);
            assertEquals("In Progress", entry.newValue);
        });
    }

    @Test
    @DisplayName("Should persist assignee change including assignment from unassigned")
    void shouldLogAssigneeAssignment() {
        Given.transaction(() -> {
            var ticket = ticketRepository.findById(fixtures.ticket().id()).orElseThrow();
            var user = Given.user("user@issues.vepo.dev");

            historyService.logAssigneeChanged(ticket, user, null, "New Assignee");

            var entry = historyRepository.findByTicketId(ticket.getId())
                                         .stream()
                                         .filter(e -> e.action == TicketHistoryAction.ASSIGNEE_CHANGED)
                                         .findFirst()
                                         .orElseThrow();
            assertEquals("assignee", entry.field);
            assertNull(entry.oldValue);
            assertEquals("New Assignee", entry.newValue);
        });
    }

    @Test
    @DisplayName("Should persist subscribe and unsubscribe events")
    void shouldLogSubscribeAndUnsubscribe() {
        Given.transaction(() -> {
            var ticket = ticketRepository.findById(fixtures.ticket().id()).orElseThrow();
            var user = Given.user("user@issues.vepo.dev");

            historyService.logSubscribed(ticket, user, "Observer");
            historyService.logUnsubscribed(ticket, user, "Observer");

            var entries = historyRepository.findByTicketId(ticket.getId());
            assertEquals(2,
                         entries.stream()
                                .filter(e -> e.action == TicketHistoryAction.SUBSCRIBED
                                        || e.action == TicketHistoryAction.UNSUBSCRIBED)
                                .count());
        });
    }
}
