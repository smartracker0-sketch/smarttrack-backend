package com.trackpro.dto.telemetry;

import java.time.Instant;
import java.util.UUID;

public record DeviceAlertDto(
        UUID id,
        UUID deviceId,
        Instant alertTime,
        Instant receivedAt,
        String alertType,
        String severity,
        String message,
        boolean acknowledged,
        Instant ackAt,
        Double latitude,
        Double longitude
) {}
