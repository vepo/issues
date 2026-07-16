package dev.vepo.issues.ticket.history;

import java.time.Instant;

import dev.vepo.issues.auth.apitoken.ApiTokenIdentityProvider;
import dev.vepo.issues.ticket.Ticket;
import dev.vepo.issues.user.User;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TicketHistoryService {

    private final TicketHistoryRepository historyRepository;
    private final SecurityIdentity securityIdentity;

    @Inject
    public TicketHistoryService(TicketHistoryRepository historyRepository, SecurityIdentity securityIdentity) {
        this.historyRepository = historyRepository;
        this.securityIdentity = securityIdentity;
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

    public void logPriorityChanged(Ticket ticket, User user, String fromPriority, String toPriority) {
        createEntry(ticket, user, TicketHistoryAction.FIELD_CHANGED, "priority", fromPriority, toPriority, null);
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

    public void logLinkAdded(Ticket ticket, User user, String linkType, String otherIdentifier, Long linkId) {
        createEntry(ticket, user, TicketHistoryAction.LINK_ADDED, linkType, null, otherIdentifier, linkId);
    }

    public void logLinkRemoved(Ticket ticket, User user, String linkType, String otherIdentifier, Long linkId) {
        createEntry(ticket, user, TicketHistoryAction.LINK_REMOVED, linkType, otherIdentifier, null, linkId);
    }

    public void logAttachmentAdded(Ticket ticket, User user, String originalFilename, Long attachmentId) {
        createEntry(ticket, user, TicketHistoryAction.ATTACHMENT_ADDED, null, null, originalFilename, attachmentId);
    }

    public void logAttachmentRemoved(Ticket ticket, User user, String originalFilename, Long attachmentId) {
        createEntry(ticket, user, TicketHistoryAction.ATTACHMENT_REMOVED, null, originalFilename, null, attachmentId);
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
                                        Instant.now(),
                                        isViaAgent());
        historyRepository.save(history);
    }

    private boolean isViaAgent() {
        return Boolean.TRUE.equals(securityIdentity.getAttribute(ApiTokenIdentityProvider.VIA_AGENT_ATTRIBUTE));
    }
}
