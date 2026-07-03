package dev.vepo.issues.ticket.search.saved;

import java.util.List;

import dev.vepo.issues.ticket.TicketResponse;

public record SavedQueryWithResultsResponse(SavedQueryResponse savedQuery, List<TicketResponse> tickets) {
    public static SavedQueryWithResultsResponse load(SavedQuery savedQuery, List<TicketResponse> tickets) {
        return new SavedQueryWithResultsResponse(SavedQueryResponse.load(savedQuery), tickets);
    }
}
