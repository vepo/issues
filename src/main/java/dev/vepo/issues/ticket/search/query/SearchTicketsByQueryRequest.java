package dev.vepo.issues.ticket.search.query;

import jakarta.validation.constraints.NotBlank;

public record SearchTicketsByQueryRequest(@NotBlank String query) {}
