package dev.vepo.issues.ticket.csvimport;

import java.util.List;

public record PreviewTicketImportResponse(List<ImportRowValidation> rows, int validCount, int invalidCount) {}
