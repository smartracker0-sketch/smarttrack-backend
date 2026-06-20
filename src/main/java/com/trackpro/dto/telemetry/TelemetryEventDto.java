package com.trackpro.dto.telemetry;

import java.time.Instant;
import java.util.UUID;

public record TelemetryEventDto(
        UUID id,
        UUID deviceId,
        String imei,
        Instant eventTime,
        Instant receivedAt,
        Double latitude,
        Double longitude,
        Double altitudeM,
        Double speedKph,
        Double headingDeg,
        Double accuracyM,
        Integer satellites,
        Boolean ignition,
        Integer voltageMv,
        Integer gsmSignal,
        Long odometerM
) {}
