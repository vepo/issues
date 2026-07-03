package dev.vepo.issues.phase;

import java.util.List;

public record VersionChangelogSection(String name, List<VersionChangelogEntry> tickets) {}
