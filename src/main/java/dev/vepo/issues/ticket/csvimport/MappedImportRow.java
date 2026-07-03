package dev.vepo.issues.ticket.csvimport;

import dev.vepo.issues.ticket.TicketPriority;

public record MappedImportRow(int rowNumber,
                              String title,
                              String description,
                              String categoryName,
                              TicketPriority priority,
                              String assigneeEmail,
                              String statusName,
                              String projectName) {}
