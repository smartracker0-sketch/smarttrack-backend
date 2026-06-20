package com.trackpro.dto.telemetry;

import com.trackpro.alert.AlertSeverity;
import com.trackpro.alert.AlertType;
import java.time.Instant;
import java.util.UUID;

public record DeviceAlertDto(
        UUID id,
        UUID deviceId,
        Instant alertTime,
        Instant receivedAt,
        AlertType alertType,
        AlertSeverity severity,
        String message,
        boolean acknowledged,
        Instant ackAt,
        Double latitude,
        Double longitude,
        Double speedKph,
        Long durationSeconds,
        UUID relatedGeofenceId,
        String relatedGeofenceName
) {}
