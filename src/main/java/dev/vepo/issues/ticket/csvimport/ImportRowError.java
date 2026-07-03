package dev.vepo.issues.ticket.csvimport;

public record ImportRowError(int rowNumber, String message) {}
