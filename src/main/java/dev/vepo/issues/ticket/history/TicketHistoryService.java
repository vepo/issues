package dev.vepo.issues.ticket.history;

import java.time.Instant;

import dev.vepo.issues.ticket.Ticket;
import dev.vepo.issues.user.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TicketHistoryService {

    private final TicketHistoryRepository historyRepository;

    @Inject
    public TicketHistoryService(TicketHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    public void logTicketCreated(Ticket ticket, User user) {
        createEntry(ticket, user, TicketHistoryAction.CREATED, null, null, null, null);
    }

    public void logFieldChanged(Ticket ticket, User user, String field, String oldValue, String newValue) {
        createEntry(ticket, user, TicketHistoryAction.FIELD_CHANGED, field, oldValue, newValue, null);
    }

    public void logStatusChanged(Ticket ticket, User user, String fromStatus, String toStatus) {
        createEntry(ticket,
                    user,
                    TicketHistoryAction.STATUS_CHANGED,
                    "status",
                    HistoryDisplay.formatStatus(fromStatus),
                    HistoryDisplay.formatStatus(toStatus),
                    null);
    }

    public void logAssigneeChanged(Ticket ticket, User user, String fromAssignee, String toAssignee) {
        createEntry(ticket, user, TicketHistoryAction.ASSIGNEE_CHANGED, "assignee", fromAssignee, toAssignee, null);
    }

    public void logSubscribed(Ticket ticket, User user, String subscriberName) {
        createEntry(ticket, user, TicketHistoryAction.SUBSCRIBED, "subscriber", null, subscriberName, null);
    }

    public void logUnsubscribed(Ticket ticket, User user, String subscriberName) {
        createEntry(ticket, user, TicketHistoryAction.UNSUBSCRIBED, "subscriber", subscriberName, null, null);
    }

    public void logTicketDeleted(Ticket ticket, User user) {
        createEntry(ticket, user, TicketHistoryAction.DELETED, null, null, null, null);
    }

    public void logTicketRestored(Ticket ticket, User user) {
        createEntry(ticket, user, TicketHistoryAction.RESTORED, null, null, null, null);
    }

    private void createEntry(Ticket ticket,
                             User user,
                             TicketHistoryAction action,
                             String field,
                             String oldValue,
                             String newValue,
                             Long referenceId) {
        var history = new TicketHistory(ticket,
                                        user,
                                        action,
                                        field,
                                        oldValue,
                                        newValue,
                                        referenceId,
                                        Instant.now());
        historyRepository.save(history);
    }
}
