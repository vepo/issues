package dev.vepo.issues.categories;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(@NotBlank @Size(min = 2, max = 64) String name,
                                    @NotBlank @Size(min = 3, max = 16) String color) {}
