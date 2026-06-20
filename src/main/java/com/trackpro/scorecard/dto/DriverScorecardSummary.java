package com.trackpro.scorecard.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record DriverScorecardSummary(
        UUID driverId,
        String driverName,
        UUID orgId,
        BigDecimal overallScore,
        String band,
        String colour,
        int totalTrips,
        int rank,
        List<Integer> last10Scores,
        Map<String, Long> violationsByType,
        java.time.Instant lastScoredAt
) {}
