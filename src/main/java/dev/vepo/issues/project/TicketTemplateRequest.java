package dev.vepo.issues.project;

import dev.vepo.issues.ticket.TicketPriority;

public record TicketTemplateRequest(boolean enabled,
                                    String title,
                                    String description,
                                    Long categoryId,
                                    TicketPriority priority) {}
