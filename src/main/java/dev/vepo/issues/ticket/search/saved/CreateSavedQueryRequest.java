package dev.vepo.issues.ticket.search.saved;

import jakarta.validation.constraints.NotBlank;

public record CreateSavedQueryRequest(@NotBlank String name, @NotBlank String query, boolean showAtHome) {}
