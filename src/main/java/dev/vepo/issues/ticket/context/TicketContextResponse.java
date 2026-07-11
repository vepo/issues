package dev.vepo.issues.ticket.context;

import java.util.List;

import dev.vepo.issues.customfield.CustomFieldResponse;
import dev.vepo.issues.ticket.TicketExpandedResponse;

public record TicketContextResponse(TicketExpandedResponse ticket,
                                    List<TicketAvailableTransitionResponse> availableTransitions,
                                    List<CustomFieldResponse> customFields) {}
