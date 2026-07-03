package dev.vepo.issues.home;

import java.time.Instant;

import dev.vepo.issues.ticket.comments.Comment;
import dev.vepo.issues.ticket.history.TicketHistory;

public record HomeActivityResponse(String type,
                                   long ticketId,
                                   String ticketIdentifier,
                                   String ticketTitle,
                                   String projectName,
                                   String actorName,
                                   String summary,
                                   Instant occurredAt) {

    public static HomeActivityResponse fromComment(Comment comment) {
        var ticket = comment.getTicket();
        return new HomeActivityResponse("COMMENT",
                                        ticket.getId(),
                                        ticket.getIdentifier(),
                                        ticket.getTitle(),
                                        ticket.getProject().getName(),
                                        comment.getAuthor().getName(),
                                        comment.getContent(),
                                        comment.getCreatedAt());
    }

    public static HomeActivityResponse fromStatusChange(TicketHistory history) {
        var ticket = history.ticket;
        return new HomeActivityResponse("STATUS_CHANGED",
                                        ticket.getId(),
                                        ticket.getIdentifier(),
                                        ticket.getTitle(),
                                        ticket.getProject().getName(),
                                        history.user.getName(),
                                        "%s → %s".formatted(history.oldValue, history.newValue),
                                        history.timestamp);
    }
}
