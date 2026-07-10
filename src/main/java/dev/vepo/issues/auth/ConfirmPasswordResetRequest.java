package dev.vepo.issues.auth;

import jakarta.validation.constraints.NotBlank;

public record ConfirmPasswordResetRequest(@NotBlank String token,
                                          @StrongPassword String newPassword) {}
