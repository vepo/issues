package dev.vepo.issues.ticket.csvimport;

import java.util.List;

public record PreviewTicketImportResponse(List<ImportRowValidationResponse> rows, int validCount, int invalidCount) {}
