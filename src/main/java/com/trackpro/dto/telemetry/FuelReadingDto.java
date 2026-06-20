package com.trackpro.dto.telemetry;

import java.time.Instant;
import java.util.UUID;

public record FuelReadingDto(
        UUID id,
        UUID deviceId,
        Instant eventTime,
        Instant receivedAt,
        Double fuelLevelPct,
        Double fuelLiters,
        Double temperatureC,
        String tankId
) {}
