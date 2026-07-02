package dev.vepo.issues.ticket.business;

import java.time.Instant;

import dev.vepo.issues.ticket.Ticket;
import dev.vepo.issues.ticket.history.TicketHistory;
import dev.vepo.issues.ticket.history.TicketHistoryRepository;
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

    /**
     * Logs ticket creation
     */
    public void logTicketCreated(Ticket ticket, User user) {
        createHistoryEntry(ticket, user, "Ticket created");
    }

    /**
     * Logs ticket update with field changes
     */
    public void logTicketUpdated(Ticket ticket, User user, String changes) {
        createHistoryEntry(ticket, user, "Ticket updated: " + changes);
    }

    /**
     * Logs ticket status change
     */
    public void logStatusChanged(Ticket ticket, User user, String fromStatus, String toStatus) {
        createHistoryEntry(ticket, user, "Status changed from '%s' to '%s'".formatted(fromStatus, toStatus));
    }

    /**
     * Logs assignee change
     */
    public void logAssigneeChanged(Ticket ticket, User user, String fromAssignee, String toAssignee) {
        String description;
        if (fromAssignee == null && toAssignee != null) {
            description = "Assigned to '%s'".formatted(toAssignee);
        } else if (fromAssignee != null && toAssignee == null) {
            description = "Unassigned";
        } else {
            description = "Assignee changed from '%s' to '%s'".formatted(fromAssignee, toAssignee);
        }
        createHistoryEntry(ticket, user, description);
    }

    /**
     * Logs category change
     */
    public void logCategoryChanged(Ticket ticket, User user, String fromCategory, String toCategory) {
        createHistoryEntry(ticket, user, "Category changed from '%s' to '%s'".formatted(fromCategory, toCategory));
    }

    /**
     * Logs comment addition
     */
    public void logCommentAdded(Ticket ticket, User user) {
        createHistoryEntry(ticket, user, "Comment added");
    }

    /**
     * Logs ticket deletion
     */
    public void logTicketDeleted(Ticket ticket, User user) {
        createHistoryEntry(ticket, user, "Ticket deleted");
    }

    /**
     * Logs ticket restoration (if soft delete is implemented)
     */
    public void logTicketRestored(Ticket ticket, User user) {
        createHistoryEntry(ticket, user, "Ticket restored");
    }

    /**
     * Logs priority change (if priority field is added)
     */
    public void logPriorityChanged(Ticket ticket, User user, String fromPriority, String toPriority) {
        createHistoryEntry(ticket, user, "Priority changed from '%s' to '%s'".formatted(fromPriority, toPriority));
    }

    /**
     * Logs due date change (if due date field is added)
     */
    public void logDueDateChanged(Ticket ticket, User user, String fromDueDate, String toDueDate) {
        createHistoryEntry(ticket, user, "Due date changed from '%s' to '%s'".formatted(fromDueDate, toDueDate));
    }

    /**
     * Logs custom action
     */
    public void logCustomAction(Ticket ticket, User user, String action) {
        createHistoryEntry(ticket, user, action);
    }

    /**
     * Creates and saves a history entry
     */
    private void createHistoryEntry(Ticket ticket, User user, String description) {
        TicketHistory history = new TicketHistory(ticket, user, description, Instant.now());
        historyRepository.save(history);
    }
}