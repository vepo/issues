package dev.vepo.issues.categories;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(@NotBlank @Size(min = 2, max = 64) String name,
                                    @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a hex value like #00635D") String color) {}
