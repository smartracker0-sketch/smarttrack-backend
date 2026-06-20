package com.trackpro.dto.organisation;

import java.time.Instant;
import java.util.UUID;

public record OrganisationDto(
        UUID id,
        String name,
        String slug,
        String plan,
        String status,
        String adminEmail,
        int vehicleLimit,
        Instant createdAt,
        Instant updatedAt
) {}
