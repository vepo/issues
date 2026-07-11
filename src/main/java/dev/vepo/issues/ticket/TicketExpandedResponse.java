package dev.vepo.issues.ticket;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import dev.vepo.issues.customfield.CustomFieldValueResponse;
import dev.vepo.issues.ticket.history.TicketHistory;
import dev.vepo.issues.ticket.link.ChildrenSummaryResponse;
import dev.vepo.issues.ticket.link.TicketLinkResponse;

public record TicketExpandedResponse(long id,
                                     String identifier,
                                     String title,
                                     String description,
                                     String category,
                                     String categoryColor,
                                     String priority,
                                     String ticketType,
                                     TicketUserResponse author,
                                     TicketUserResponse assignee,
                                     List<TicketUserResponse> subscribers,
                                     TicketProjectResponse project,
                                     String status,
                                     LocalDateTime finishedAt,
                                     LocalDateTime canceledAt,
                                     LocalDate dueDate,
                                     Integer storyPoints,
                                     Long observedVersionId,
                                     String observedVersionLabel,
                                     Long targetVersionId,
                                     String targetVersionLabel,
                                     Long phaseId,
                                     String phaseName,
                                     boolean deleted,
                                     List<TicketHistoryResponse> history,
                                     List<CustomFieldValueResponse> customFields,
                                     List<TicketLinkResponse> links,
                                     ChildrenSummaryResponse childrenSummary) {

    public static TicketExpandedResponse load(Ticket ticket, List<TicketHistory> history) {
        return load(ticket, history, List.of(), List.of(), new ChildrenSummaryResponse(0, 0));
    }

    public static TicketExpandedResponse load(Ticket ticket,
                                              List<TicketHistory> history,
                                              List<CustomFieldValueResponse> customFields) {
        return load(ticket, history, customFields, List.of(), new ChildrenSummaryResponse(0, 0));
    }

    public static TicketExpandedResponse load(Ticket ticket,
                                              List<TicketHistory> history,
                                              List<CustomFieldValueResponse> customFields,
                                              List<TicketLinkResponse> links,
                                              ChildrenSummaryResponse childrenSummary) {
        return new TicketExpandedResponse(ticket.getId(),
                                          ticket.getIdentifier(),
                                          ticket.getTitle(),
                                          ticket.getDescription(),
                                          ticket.getCategory().getName(),
                                          ticket.getCategory().getColor(),
                                          ticket.getPriority().name(),
                                          ticket.getTicketType() != null ? ticket.getTicketType().name() : TicketType.TASK.name(),
                                          TicketUserResponse.load(ticket.getAuthor()),
                                          TicketUserResponse.load(ticket.getAssignee()),
                                          ticket.getSubscribers()
                                                .stream()
                                                .map(TicketUserResponse::load)
                                                .toList(),
                                          TicketProjectResponse.load(ticket.getProject()),
                                          ticket.getStatus().getName(),
                                          ticket.getFinishedAt(),
                                          ticket.getCanceledAt(),
                                          ticket.getDueDate(),
                                          ticket.getStoryPoints(),
                                          ticket.getObservedVersion() != null ? ticket.getObservedVersion().getId() : null,
                                          ticket.getObservedVersion() != null ? ticket.getObservedVersion().getLabel() : null,
                                          ticket.getTargetVersion() != null ? ticket.getTargetVersion().getId() : null,
                                          ticket.getTargetVersion() != null ? ticket.getTargetVersion().getLabel() : null,
                                          ticket.getPhase() != null ? ticket.getPhase().getId() : null,
                                          ticket.getPhase() != null ? ticket.getPhase().getName() : null,
                                          ticket.isDeleted(),
                                          history.stream()
                                                 .map(TicketHistoryResponse::load)
                                                 .toList(),
                                          customFields == null ? List.of() : List.copyOf(customFields),
                                          links == null ? List.of() : List.copyOf(links),
                                          childrenSummary == null ? new ChildrenSummaryResponse(0, 0) : childrenSummary);
    }

}
