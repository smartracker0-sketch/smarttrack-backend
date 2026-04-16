package com.trackpro.dto.device;

import java.time.Instant;
import java.util.UUID;

public record DeviceDto(
        UUID id,
        String imei,
        String name,
        Instant createdAt
) {
}
