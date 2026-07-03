package dev.vepo.issues.ticket.search.saved;

import jakarta.validation.constraints.NotBlank;

public record UpdateSavedQueryRequest(@NotBlank String name, @NotBlank String query, boolean showAtHome) {}
