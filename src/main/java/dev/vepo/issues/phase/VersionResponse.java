package dev.vepo.issues.phase;

public record VersionResponse(long id, long projectId, String label, String description) {
    public static VersionResponse load(Version version) {
        return new VersionResponse(version.getId(),
                                   version.getProject().getId(),
                                   version.getLabel(),
                                   version.getDescription());
    }
}
