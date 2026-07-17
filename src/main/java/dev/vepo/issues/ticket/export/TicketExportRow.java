package dev.vepo.issues.ticket.export;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import dev.vepo.issues.ticket.TicketPriority;
import dev.vepo.issues.ticket.TicketType;

@SuppressWarnings("java:S107")
public record TicketExportRow(String identifier,
                              String title,
                              String description,
                              String projectKey,
                              String projectName,
                              String statusCode,
                              String statusName,
                              Long categoryId,
                              String categoryName,
                              TicketPriority priority,
                              TicketType type,
                              String authorEmail,
                              String authorName,
                              String assigneeEmail,
                              String assigneeName,
                              Long phaseId,
                              String phaseName,
                              Long observedVersionId,
                              String observedVersionName,
                              Long targetVersionId,
                              String targetVersionName,
                              Integer storyPoints,
                              LocalDate dueDate,
                              LocalDateTime createdAt,
                              LocalDateTime updatedAt,
                              Map<String, Object> customFields) {

    public TicketExportRow {
        customFields = Collections.unmodifiableMap(new LinkedHashMap<>(customFields));
    }
}
