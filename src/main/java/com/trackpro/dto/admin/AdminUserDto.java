package com.trackpro.dto.admin;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminUserDto(
        UUID id,
        String email,
        String displayName,
        List<String> roles,
        boolean enabled,
        UUID organisationId,
        String organisationName,
        Instant createdAt
) {}
