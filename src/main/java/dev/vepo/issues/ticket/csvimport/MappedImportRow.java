package dev.vepo.issues.ticket.csvimport;

import java.util.Map;

import dev.vepo.issues.ticket.TicketPriority;

public record MappedImportRow(int rowNumber,
                              String title,
                              String description,
                              String categoryName,
                              TicketPriority priority,
                              Integer storyPoints,
                              String assigneeEmail,
                              String statusName,
                              String projectName,
                              Map<String, String> customFieldValues) {

    public MappedImportRow {
        customFieldValues = customFieldValues == null ? Map.of() : Map.copyOf(customFieldValues);
    }
}
