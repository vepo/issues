package dev.vepo.issues.dashboards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import jakarta.ws.rs.BadRequestException;

class DashboardTypeTest {

    @Test
    void shouldParseKebabCaseIds() {
        assertEquals(DashboardType.TICKETS_BY_STATUS, DashboardType.fromString("tickets-by-status"));
        assertEquals(DashboardType.PERFORMANCE_KPI, DashboardType.fromString("performance-kpi"));
    }

    @Test
    void shouldParseEnumNames() {
        assertEquals(DashboardType.TICKETS_BY_STATUS, DashboardType.fromString("TICKETS_BY_STATUS"));
        assertEquals(DashboardType.TICKETS_BY_PRIORITY, DashboardType.fromString("tickets_by_priority"));
    }

    @Test
    void shouldRejectInvalidTypeWithCorrectMessage() {
        var error = assertThrows(BadRequestException.class, () -> DashboardType.fromString("not-a-widget"));
        assertEquals("Invalid dashboard type! type=not-a-widget", error.getMessage());
    }
}
