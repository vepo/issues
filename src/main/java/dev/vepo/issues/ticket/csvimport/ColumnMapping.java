package dev.vepo.issues.ticket.csvimport;

public record ColumnMapping(String titleColumn,
                            String descriptionColumn,
                            String categoryColumn,
                            String priorityColumn,
                            String assigneeEmailColumn,
                            String statusColumn,
                            String projectColumn) {}
