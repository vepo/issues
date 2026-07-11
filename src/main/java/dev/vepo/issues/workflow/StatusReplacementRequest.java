package dev.vepo.issues.workflow;

import jakarta.validation.constraints.NotBlank;

public record StatusReplacementRequest(@NotBlank(message = "Removed status name is required") String from,
                                       @NotBlank(message = "Replacement status name is required") String to) {}
