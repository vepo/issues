package dev.vepo.issues.phase;

import java.util.List;

public record VersionChangelogResponse(long versionId, String label, String description, List<VersionChangelogSection> sections) {}
