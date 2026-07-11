package dev.vepo.issues.project;

import java.util.List;

import dev.vepo.issues.customfield.CustomFieldValueRequest;
import dev.vepo.issues.ticket.TicketPriority;

public record TicketTemplateRequest(boolean enabled,
                                    String title,
                                    String description,
                                    Long categoryId,
                                    TicketPriority priority,
                                    List<CustomFieldValueRequest> customFieldDefaults) {}
