package dev.vepo.issues.ticket;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import dev.vepo.issues.customfield.CustomFieldValueResponse;

public record TicketResponse(long id,
                             String identifier,
                             String title,
                             String description,
                             long category,
                             String categoryName,
                             String categoryColor,
                             long author,
                             Long assignee,
                             long project,
                             long status,
                             String priority,
                             String ticketType,
                             LocalDateTime finishedAt,
                             LocalDate dueDate,
                             Long observedVersionId,
                             String observedVersionLabel,
                             Long targetVersionId,
                             String targetVersionLabel,
                             Long phaseId,
                             String phaseName,
                             boolean deleted,
                             List<CustomFieldValueResponse> customFields) {
    public static TicketResponse load(Ticket ticket) {
        return load(ticket, List.of());
    }

    public static TicketResponse load(Ticket ticket, List<CustomFieldValueResponse> customFields) {
        return new TicketResponse(ticket.getId(),
                                  ticket.getIdentifier(),
                                  ticket.getTitle(),
                                  ticket.getDescription(),
                                  ticket.getCategory().getId(),
                                  ticket.getCategory().getName(),
                                  ticket.getCategory().getColor(),
                                  ticket.getAuthor().getId(),
                                  ticket.getAssignee() != null ? ticket.getAssignee().getId() : null,
                                  ticket.getProject().getId(),
                                  ticket.getStatus().getId(),
                                  ticket.getPriority().name(),
                                  ticket.getTicketType() != null ? ticket.getTicketType().name() : TicketType.TASK.name(),
                                  ticket.getFinishedAt(),
                                  ticket.getDueDate(),
                                  ticket.getObservedVersion() != null ? ticket.getObservedVersion().getId() : null,
                                  ticket.getObservedVersion() != null ? ticket.getObservedVersion().getLabel() : null,
                                  ticket.getTargetVersion() != null ? ticket.getTargetVersion().getId() : null,
                                  ticket.getTargetVersion() != null ? ticket.getTargetVersion().getLabel() : null,
                                  ticket.getPhase() != null ? ticket.getPhase().getId() : null,
                                  ticket.getPhase() != null ? ticket.getPhase().getName() : null,
                                  ticket.isDeleted(),
                                  customFields == null ? List.of() : List.copyOf(customFields));
    }
}
