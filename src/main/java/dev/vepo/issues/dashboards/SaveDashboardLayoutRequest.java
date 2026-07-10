package dev.vepo.issues.dashboards;

import java.util.List;

import jakarta.validation.constraints.NotNull;

public record SaveDashboardLayoutRequest(@NotNull List<String> widgetIds) {}
