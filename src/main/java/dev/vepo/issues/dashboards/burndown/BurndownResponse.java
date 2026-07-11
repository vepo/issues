package dev.vepo.issues.dashboards.burndown;

import java.time.LocalDate;
import java.util.List;

public record BurndownResponse(long phaseId,
                               String phaseName,
                               LocalDate startDate,
                               LocalDate endDate,
                               boolean datesComplete,
                               List<BurndownSeriesPoint> series,
                               List<BurndownWarning> warnings,
                               int commitmentPoints,
                               int remainingPoints) {}
