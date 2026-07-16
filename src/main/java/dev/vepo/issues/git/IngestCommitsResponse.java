package dev.vepo.issues.git;

public record IngestCommitsResponse(int linked, int skippedDuplicates, int unresolvedIdentifiers) {}
