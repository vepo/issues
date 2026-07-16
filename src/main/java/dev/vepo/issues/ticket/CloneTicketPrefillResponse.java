package dev.vepo.issues.ticket;

import java.util.List;

import dev.vepo.issues.customfield.CustomFieldValueResponse;

public record CloneTicketPrefillResponse(String sourceIdentifier,
                                         long targetProjectId,
                                         String title,
                                         String description,
                                         long categoryId,
                                         String priority,
                                         String ticketType,
                                         List<CustomFieldValueResponse> customFields,
                                         List<String> warnings) {}
