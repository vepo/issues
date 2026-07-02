package dev.vepo.issues.ticket;

import jakarta.validation.constraints.NotNull;

public record UpdateAssigneeRequest(@NotNull Long assigneeId) {}