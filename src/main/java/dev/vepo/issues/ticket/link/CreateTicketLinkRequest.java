package dev.vepo.issues.ticket.link;

import jakarta.validation.constraints.NotNull;

public record CreateTicketLinkRequest(@NotNull Long targetTicketId,
                                      @NotNull TicketLinkType linkType) {}
