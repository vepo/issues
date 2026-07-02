package dev.vepo.issues.dashboards;

import java.util.Map;

public record KpiDataResponse(int total, Map<String, Integer> perStatus) {}
