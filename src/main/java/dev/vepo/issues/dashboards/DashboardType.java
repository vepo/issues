package dev.vepo.issues.dashboards;

import java.util.stream.Stream;

import jakarta.ws.rs.BadRequestException;

public enum DashboardType {
    TICKETS_BY_DAY("tickets-by-day"),
    TICKETS_BY_STATUS("tickets-by-status"),
    TICKETS_BY_PRIORITY("tickets-by-priority"),
    PERFORMANCE_KPI("performance-kpi"),
    RECENT_TICKETS("recent-tickets");

    private String id;

    private DashboardType(String id) {
        this.id = id;
    }

    public static DashboardType fromString(String value) {
        return Stream.of(values())
                     .filter(type -> type.id.equalsIgnoreCase(value))
                     .findFirst()
                     .orElseThrow(() -> new BadRequestException("Invalid bashboard type! type=" + value));
    }
}
