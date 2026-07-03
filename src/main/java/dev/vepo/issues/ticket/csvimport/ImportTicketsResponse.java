package dev.vepo.issues.ticket.csvimport;

import java.util.List;

import dev.vepo.issues.ticket.TicketResponse;

public record ImportTicketsResponse(List<TicketResponse> created,
                                    List<ImportRowError> errors,
                                    ImportTicketsSummary summary) {}
