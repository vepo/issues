package dev.vepo.issues.dashboards.burndown;

public record BurndownWarning(long ticketId, String identifier, String code) {
    public static final String MISSING_STORY_POINTS = "MISSING_STORY_POINTS";
}
