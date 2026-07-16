package dev.vepo.issues.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateProfileRequest(@NotBlank(message = "Name must not be empty!") String name,
                                   @NotBlank(message = "Email must not be empty!") @Email(message = "Email must be valid!") String email,
                                   String locale) {}
