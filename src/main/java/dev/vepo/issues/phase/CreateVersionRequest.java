package dev.vepo.issues.phase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateVersionRequest(@NotBlank @Size(max = 64) String label, String description) {}
