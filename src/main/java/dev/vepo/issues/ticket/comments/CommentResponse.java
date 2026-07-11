package dev.vepo.issues.ticket.comments;

import dev.vepo.issues.ticket.TicketUserResponse;

public record CommentResponse(long id,
                              TicketUserResponse author,
                              String content,
                              long createdAt,
                              boolean viaAgent) {
    public static CommentResponse load(Comment comment) {
        return new CommentResponse(comment.getId(),
                                   TicketUserResponse.load(comment.getAuthor()),
                                   comment.getContent(),
                                   comment.getCreatedAt().toEpochMilli(),
                                   comment.isViaAgent());
    }
}