package dev.vepo.issues.ticket;

import java.util.List;

import dev.vepo.issues.ticket.history.TicketHistory;

public record TicketExpandedResponse(long id,
                                     String identifier,
                                     String title,
                                     String description,
                                     String category,
                                     String categoryColor,
                                     String priority,
                                     TicketUserResponse author,
                                     TicketUserResponse assignee,
                                     List<TicketUserResponse> subscribers,
                                     TicketProjectResponse project,
                                     String status,
                                     List<TicketHistoryResponse> history) {

    public static TicketExpandedResponse load(Ticket ticket, List<TicketHistory> history) {
        return new TicketExpandedResponse(ticket.getId(),
                                          ticket.getIdentifier(),
                                          ticket.getTitle(),
                                          ticket.getDescription(),
                                          ticket.getCategory().getName(),
                                          ticket.getCategory().getColor(),
                                          ticket.getPriority().name(),
                                          TicketUserResponse.load(ticket.getAuthor()),
                                          TicketUserResponse.load(ticket.getAssignee()),
                                          ticket.getSubscribers()
                                                .stream()
                                                .map(TicketUserResponse::load)
                                                .toList(),
                                          TicketProjectResponse.load(ticket.getProject()),
                                          ticket.getStatus().getName(),
                                          history.stream()
                                                 .map(TicketHistoryResponse::load)
                                                 .toList());
    }

}