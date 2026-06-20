package com.trackpro.scorecard.dto;

import java.util.List;
import java.util.UUID;

public record TripScoreBreakdown(
        UUID tripId,
        UUID driverId,
        int startingScore,
        List<ScoreEvent> events,
        int cleanTripBonus,
        int streakBonus,
        int finalScore,
        int adjustedScore,
        String adjustmentReason,
        String band,
        String colour
) {

    public record ScoreEvent(
            String type,
            int points,
            String reason,
            String timestamp
    ) {}
}
