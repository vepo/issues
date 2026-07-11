package dev.vepo.issues.ticket.context;

public record TicketAvailableTransitionResponse(long toStatusId, String toStatusName) {}
