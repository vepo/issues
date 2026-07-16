package dev.vepo.issues.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(@NotBlank @Size(min = 4, max = 15) String username,
                                  @NotBlank String name,
                                  @NotBlank @Email String email,
                                  @StrongPassword String password,
                                  String locale) {}
