package dev.vepo.issues.ticket.link;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import dev.vepo.issues.infra.PlainTextSize;

public record CreateChildTicketRequest(@NotNull @Size(min = 5, max = 255) String title,
                                       @PlainTextSize(min = 0, max = 1200) String description) {}
