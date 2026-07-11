package dev.vepo.issues.ticket.backlog;

import jakarta.validation.constraints.NotNull;

public record ReorderBacklogRequest(@NotNull Long ticketId, Long beforeTicketId) {}
