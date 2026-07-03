package dev.vepo.issues.ticket.search.saved;

import java.util.List;

import dev.vepo.issues.ticket.TicketResponse;

public record HomeSavedQuerySectionResponse(SavedQueryResponse savedQuery, List<TicketResponse> tickets) {
    public static HomeSavedQuerySectionResponse load(SavedQuery savedQuery, List<TicketResponse> tickets) {
        return new HomeSavedQuerySectionResponse(SavedQueryResponse.load(savedQuery), tickets);
    }
}
