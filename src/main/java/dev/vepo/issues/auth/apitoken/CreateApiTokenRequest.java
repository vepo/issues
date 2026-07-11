package dev.vepo.issues.auth.apitoken;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateApiTokenRequest(
                                    @NotBlank @Size(max = 255) String name) {}
