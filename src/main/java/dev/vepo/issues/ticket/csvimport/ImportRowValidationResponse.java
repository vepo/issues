package dev.vepo.issues.ticket.csvimport;

import java.util.List;

public record ImportRowValidationResponse(long rowId,
                                          int rowNumber,
                                          boolean valid,
                                          MappedImportRow preview,
                                          List<String> errors) {}
