package dev.vepo.issues.customfield;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EnumOptionRequest(@NotBlank @Size(max = 128) String value,
                                @NotBlank @Size(max = 128) String label,
                                Integer sortOrder) {}
