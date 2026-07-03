package dev.vepo.issues.ticket.csvimport;

import java.util.List;
import java.util.Map;

public record TicketImportUploadResponse(long id,
                                         String fileName,
                                         List<String> headers,
                                         int rowCount,
                                         boolean truncated,
                                         List<Map<String, String>> sampleRows,
                                         boolean projectScoped) {}
