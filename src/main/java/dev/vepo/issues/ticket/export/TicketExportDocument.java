package dev.vepo.issues.ticket.export;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import dev.vepo.issues.ticket.TicketPriority;
import dev.vepo.issues.ticket.TicketType;

@JsonPropertyOrder({ "schemaVersion", "generatedAt", "source", "count", "tickets" })
public record TicketExportDocument(int schemaVersion,
                                   Instant generatedAt,
                                   ExportSource source,
                                   int count,
                                   List<Ticket> tickets) {

    public TicketExportDocument {
        tickets = List.copyOf(tickets);
    }

    @SuppressWarnings("java:S107")
    @JsonPropertyOrder({ "identifier", "title", "description", "projectKey", "projectName", "statusCode", "statusName", "categoryId", "categoryName", "priority", "type", "authorEmail", "authorName", "assigneeEmail", "assigneeName", "phaseId", "phaseName", "observedVersionId", "observedVersionName", "targetVersionId", "targetVersionName", "storyPoints", "dueDate", "createdAt", "updatedAt", "customFields" })
    public record Ticket(String identifier,
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

        public Ticket {
            customFields = Collections.unmodifiableMap(new LinkedHashMap<>(customFields));
        }
    }
}
