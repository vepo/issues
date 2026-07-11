package dev.vepo.issues.customfield;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomFieldValueRequest(@NotBlank @Size(max = 32) String key, Object value) {}
