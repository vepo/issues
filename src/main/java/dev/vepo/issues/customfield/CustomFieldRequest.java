package dev.vepo.issues.customfield;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CustomFieldRequest(@NotBlank @Size(max = 32) String key,
                                 @NotBlank @Size(max = 128) String label,
                                 @NotNull CustomFieldType type,
                                 boolean required,
                                 Boolean enabled,
                                 Integer stringMaxLength,
                                 Integer integerMin,
                                 Integer integerMax,
                                 Integer sortOrder,
                                 @Valid List<EnumOptionRequest> enumOptions,
                                 List<@NotBlank String> statusRequired) {}
