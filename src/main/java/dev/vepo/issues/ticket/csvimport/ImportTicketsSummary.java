package dev.vepo.issues.ticket.csvimport;

import java.util.List;

public record ImportTicketsSummary(int importedCount,
                                   int projectsImpacted,
                                   List<ImportCountByName> ticketsByProject,
                                   List<ImportCountByName> ticketsByStatus) {}
