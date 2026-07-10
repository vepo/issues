package dev.vepo.issues.workflow;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StatusWipRequest(@NotBlank String status, @NotNull @Min(1) Integer wipLimit) {}
