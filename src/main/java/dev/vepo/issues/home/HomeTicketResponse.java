package dev.vepo.issues.home;

import java.time.LocalDateTime;

import dev.vepo.issues.ticket.Ticket;
import dev.vepo.issues.ticket.comments.Comment;
import dev.vepo.issues.ticket.history.TicketHistory;

public record HomeTicketResponse(long id,
                                 String identifier,
                                 String title,
                                 long projectId,
                                 String projectName,
                                 String status,
                                 String priority,
                                 LocalDateTime updatedAt) {

    public static HomeTicketResponse load(Ticket ticket) {
        return new HomeTicketResponse(ticket.getId(),
                                      ticket.getIdentifier(),
                                      ticket.getTitle(),
                                      ticket.getProject().getId(),
                                      ticket.getProject().getName(),
                                      ticket.getStatus().getName(),
                                      ticket.getPriority().name(),
                                      ticket.getUpdatedAt());
    }
}
