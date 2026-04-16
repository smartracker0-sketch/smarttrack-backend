package com.trackpro.dto.location;

import java.time.Instant;
import java.util.UUID;

public record LocationDto(
        UUID id,
        UUID deviceId,
        double latitude,
        double longitude,
        Double speedKph,
        Double headingDeg,
        Double accuracyM,
        Instant recordedAt,
        Instant receivedAt
) {
}
