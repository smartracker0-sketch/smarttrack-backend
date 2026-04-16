package com.trackpro.dto.location;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record LocationIngestRequest(
        @NotNull Instant recordedAt,
        @Min(-90) @Max(90) double latitude,
        @Min(-180) @Max(180) double longitude,
        Double speedKph,
        Double headingDeg,
        Double accuracyM
) {
}
