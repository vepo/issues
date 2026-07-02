package dev.vepo.issues.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank(message = "Email must not be empty!") String email,
                           @NotBlank(message = "Password must not be empty!") String password) {

}