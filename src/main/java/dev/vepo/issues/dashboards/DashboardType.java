package dev.vepo.issues.dashboards;

import java.util.Locale;
import java.util.stream.Stream;

import jakarta.ws.rs.BadRequestException;

public enum DashboardType {
    TICKETS_BY_DAY("tickets-by-day"),
    TICKETS_BY_STATUS("tickets-by-status"),
    TICKETS_BY_PRIORITY("tickets-by-priority"),
    PERFORMANCE_KPI("performance-kpi"),
    RECENT_TICKETS("recent-tickets");

    private final String id;

    DashboardType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    /**
     * Accepts kebab-case widget ids ({@code tickets-by-status}) and enum names
     * ({@code TICKETS_BY_STATUS}).
     */
    public static DashboardType fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Invalid dashboard type! type=%s".formatted(value));
        }
        return Stream.of(values())
                     .filter(type -> type.id.equalsIgnoreCase(value) || type.name().equalsIgnoreCase(value))
                     .findFirst()
                     .orElseThrow(() -> new BadRequestException("Invalid dashboard type! type=%s".formatted(value)));
    }

    public static DashboardType fromWidgetId(String widgetId) {
        return fromString(widgetId);
    }

    public String toOpenApiEnumName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
