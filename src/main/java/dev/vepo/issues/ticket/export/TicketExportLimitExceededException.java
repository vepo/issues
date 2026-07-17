package dev.vepo.issues.ticket.export;

import dev.vepo.issues.infra.IssuesException;

final class TicketExportLimitExceededException extends IssuesException {

    static final String MESSAGE = "Ticket exports are limited to 10,000 tickets";

    TicketExportLimitExceededException() {
        super(MESSAGE);
    }
}
