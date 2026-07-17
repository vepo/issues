package dev.vepo.issues.ticket.export;

sealed interface TicketExportCriteria permits SimpleTicketExportCriteria,
        AdvancedTicketExportCriteria,
        SavedTicketExportCriteria {}

record SimpleTicketExportCriteria(String term, Long statusId) implements TicketExportCriteria {}

record AdvancedTicketExportCriteria(String query) implements TicketExportCriteria {}

record SavedTicketExportCriteria(String slug) implements TicketExportCriteria {}
