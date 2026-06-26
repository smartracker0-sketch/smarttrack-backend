package com.trackpro.dto.user;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserMeResponse(
        UUID id,
        String email,
        String displayName,
        String avatarUrl,
        List<String> roles,
        Instant createdAt
) {}
