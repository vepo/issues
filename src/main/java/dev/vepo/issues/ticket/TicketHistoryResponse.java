package dev.vepo.issues.ticket;

import dev.vepo.issues.ticket.history.TicketHistory;

public final record TicketHistoryResponse(long id,
                                          String action,
                                          String field,
                                          String oldValue,
                                          String newValue,
                                          Long referenceId,
                                          TicketUserResponse user,
                                          long timestamp) {
    public static TicketHistoryResponse load(TicketHistory history) {
        return new TicketHistoryResponse(history.id,
                                         history.action.name(),
                                         history.field,
                                         history.oldValue,
                                         history.newValue,
                                         history.referenceId,
                                         TicketUserResponse.load(history.user),
                                         history.timestamp.toEpochMilli());
    }
}
