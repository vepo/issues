package dev.vepo.issues.ticket.link;

import dev.vepo.issues.ticket.Ticket;

public record TicketLinkResponse(long id,
                                 TicketLinkType linkType,
                                 String displayLabel,
                                 TicketLinkDirection direction,
                                 long otherTicketId,
                                 String otherIdentifier,
                                 String otherTitle,
                                 String otherStatus,
                                 long otherProjectId,
                                 String otherProjectPrefix,
                                 boolean otherDeleted) {

    public static TicketLinkResponse load(TicketLink link, long perspectiveTicketId) {
        var outbound = link.getSource().getId().equals(perspectiveTicketId);
        var other = outbound ? link.getTarget() : link.getSource();
        return new TicketLinkResponse(link.getId(),
                                      link.getLinkType(),
                                      link.getLinkType().displayLabel(outbound),
                                      outbound ? TicketLinkDirection.OUTBOUND : TicketLinkDirection.INBOUND,
                                      other.getId(),
                                      other.getIdentifier(),
                                      other.getTitle(),
                                      other.getStatus().getName(),
                                      other.getProject().getId(),
                                      other.getProject().getPrefix(),
                                      other.isDeleted());
    }

    public static TicketLinkResponse fromOther(TicketLink link, Ticket other, boolean outbound) {
        return new TicketLinkResponse(link.getId(),
                                      link.getLinkType(),
                                      link.getLinkType().displayLabel(outbound),
                                      outbound ? TicketLinkDirection.OUTBOUND : TicketLinkDirection.INBOUND,
                                      other.getId(),
                                      other.getIdentifier(),
                                      other.getTitle(),
                                      other.getStatus().getName(),
                                      other.getProject().getId(),
                                      other.getProject().getPrefix(),
                                      other.isDeleted());
    }
}
