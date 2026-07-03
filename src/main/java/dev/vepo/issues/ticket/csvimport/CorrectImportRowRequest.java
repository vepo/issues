package dev.vepo.issues.ticket.csvimport;

public record CorrectImportRowRequest(String projectName, String statusName, String assigneeEmail) {}
