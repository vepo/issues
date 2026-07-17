package dev.vepo.issues.ticket.export;

public record ExportTicketsRequest(ExportFormat format,
                                   ExportSource source,
                                   String term,
                                   Long statusId,
                                   String query,
                                   String savedQuerySlug) {}
