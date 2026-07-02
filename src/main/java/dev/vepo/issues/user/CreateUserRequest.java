package dev.vepo.issues.user;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(@NotNull @Size(min = 4, max = 15) @NotBlank String username,
                                @NotNull(message = "Seu nome não pode ser nulo, seu idiota!!!") @NotBlank String name,
                                @NotNull @NotBlank @Email String email,
                                @NotNull @NotEmpty List<String> roles) {}