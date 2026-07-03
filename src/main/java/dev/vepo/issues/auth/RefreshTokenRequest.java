package dev.vepo.issues.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(@NotBlank(message = "Refresh token must not be empty!") String refreshToken) {}
