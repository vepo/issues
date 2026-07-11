package dev.vepo.issues.dashboards.burndown;

import java.time.LocalDate;
import java.util.List;

public record BurndownSeriesPoint(LocalDate date, double ideal, int remaining) {}
