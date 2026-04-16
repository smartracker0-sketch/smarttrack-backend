package com.trackpro.dto.auth;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AuthResponse(
        UUID userId,
        String email,
        String displayName,
        List<String> roles,
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt
) {
}
