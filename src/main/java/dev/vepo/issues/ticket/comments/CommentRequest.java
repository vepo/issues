package dev.vepo.issues.ticket.comments;

public record CommentRequest(String content,
                             Long authorId) {}