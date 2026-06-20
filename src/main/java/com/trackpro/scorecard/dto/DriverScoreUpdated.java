package com.trackpro.scorecard.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record DriverScoreUpdated(
        UUID driverId,
        BigDecimal newScoreTotal,
        String newBand,
        UUID tripId,
        int finalTripScore
) {}
