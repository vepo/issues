package dev.vepo.issues.ticket.csvimport;

import java.util.List;

public record ImportRowValidation(long rowId,
                                  int rowNumber,
                                  boolean valid,
                                  MappedImportRow preview,
                                  List<String> errors) {}
