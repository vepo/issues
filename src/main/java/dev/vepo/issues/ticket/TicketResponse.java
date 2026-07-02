package dev.vepo.issues.ticket;

public record TicketResponse(long id,
                             String identifier,
                             String title,
                             String description,
                             long category,
                             long author,
                             Long assignee,
                             long project,
                             long status) {
    public static TicketResponse load(Ticket ticket) {
        return new TicketResponse(ticket.getId(),
                                  ticket.getIdentifier(),
                                  ticket.getTitle(),
                                  ticket.getDescription(),
                                  ticket.getCategory().getId(),
                                  ticket.getAuthor().getId(),
                                  ticket.getAssignee() != null ? ticket.getAssignee().getId() : null,
                                  ticket.getProject().getId(),
                                  ticket.getStatus().getId());
    }
}