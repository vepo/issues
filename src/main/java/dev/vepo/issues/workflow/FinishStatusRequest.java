package dev.vepo.issues.workflow;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FinishStatusRequest(@NotBlank String status, @NotNull FinishOutcome outcome) {}
