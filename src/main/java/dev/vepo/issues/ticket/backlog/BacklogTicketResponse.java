package dev.vepo.issues.ticket.backlog;

import dev.vepo.issues.ticket.Ticket;

public record BacklogTicketResponse(long id,
                                    String identifier,
                                    String title,
                                    long statusId,
                                    String statusName,
                                    String priority,
                                    Long assigneeId,
                                    String assigneeName,
                                    int backlogRank) {

    public static BacklogTicketResponse load(Ticket ticket) {
        var assignee = ticket.getAssignee();
        return new BacklogTicketResponse(ticket.getId(),
                                         ticket.getIdentifier(),
                                         ticket.getTitle(),
                                         ticket.getStatus().getId(),
                                         ticket.getStatus().getName(),
                                         ticket.getPriority().name(),
                                         assignee != null ? assignee.getId() : null,
                                         assignee != null ? assignee.getName() : null,
                                         ticket.getBacklogRank());
    }
}
