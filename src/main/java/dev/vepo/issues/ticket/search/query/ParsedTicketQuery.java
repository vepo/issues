package dev.vepo.issues.ticket.search.query;

import java.util.Optional;

import dev.vepo.issues.ticket.search.query.TicketQueryParser.ExpressionContext;

public record ParsedTicketQuery(Optional<TicketQueryOrder> order, ExpressionContext expression) {}
