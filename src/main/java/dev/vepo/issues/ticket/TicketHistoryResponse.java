package dev.vepo.issues.ticket;

import dev.vepo.issues.ticket.history.TicketHistory;

public final record TicketHistoryResponse(String description,
                                          TicketUserResponse user,
                                          long timestamp) {
    public static TicketHistoryResponse load(TicketHistory history) {
        return new TicketHistoryResponse(history.description,
                                         TicketUserResponse.load(history.user),
                                         history.timestamp.toEpochMilli());
    }
}