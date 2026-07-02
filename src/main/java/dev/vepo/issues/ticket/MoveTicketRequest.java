package dev.vepo.issues.ticket;

import io.smallrye.common.constraint.NotNull;

public record MoveTicketRequest(@NotNull Long to) {}