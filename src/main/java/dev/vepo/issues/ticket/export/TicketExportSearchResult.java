package dev.vepo.issues.ticket.export;

import java.util.List;

import dev.vepo.issues.ticket.Ticket;

record TicketExportSearchResult(List<Ticket> tickets, boolean explicitOrder) {

    TicketExportSearchResult {
        tickets = List.copyOf(tickets);
    }
}
