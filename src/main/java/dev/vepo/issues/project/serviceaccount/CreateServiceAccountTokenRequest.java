package dev.vepo.issues.project.serviceaccount;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateServiceAccountTokenRequest(@NotBlank @Size(max = 255) String name) {}
