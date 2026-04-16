package com.trackpro.service;

import com.trackpro.config.TrackProSecurityProperties;
import com.trackpro.dto.auth.AuthResponse;
import com.trackpro.exception.UnauthorizedException;
import com.trackpro.model.RefreshTokenEntity;
import com.trackpro.repository.RefreshTokenRepository;
import com.trackpro.repository.UserRepository;
import com.trackpro.security.JwtService;
import com.trackpro.security.TokenHasher;
import com.trackpro.security.TrackProUserPrincipal;
import jakarta.transaction.Transactional;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TrackProSecurityProperties props;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            TrackProSecurityProperties props
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.props = props;
    }

    @Transactional
    public AuthResponse login(String email, String password, Instant now) {
        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
        if (!user.isEnabled()) {
            throw new UnauthorizedException("User disabled");
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        var roles = user.getRoles().stream().map(r -> r.getName().name()).toList();
        var principal = new TrackProUserPrincipal(user.getId(), user.getEmail(), user.getPasswordHash(), user.isEnabled(), roles);

        var accessToken = jwtService.createAccessToken(principal, now);
        var refresh = issueRefreshToken(user.getId(), now);

        return new AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                roles,
                accessToken,
                jwtService.accessTokenExpiresAt(now),
                refresh.rawToken(),
                refresh.expiresAt()
        );
    }

    @Transactional
    public AuthResponse refresh(String refreshTokenRaw, Instant now) {
        String tokenHash = TokenHasher.sha256Hex(refreshTokenRaw);
        var token = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (!token.isActiveAt(now)) {
            if (token.getRevokedAt() != null) {
                refreshTokenRepository.revokeAllForUser(token.getUser().getId(), now);
            }
            throw new UnauthorizedException("Refresh token expired or revoked");
        }

        var user = token.getUser();
        if (!user.isEnabled()) {
            throw new UnauthorizedException("User disabled");
        }

        var refresh = rotateRefreshToken(token, now);
        var roles = user.getRoles().stream().map(r -> r.getName().name()).toList();
        var principal = new TrackProUserPrincipal(user.getId(), user.getEmail(), user.getPasswordHash(), user.isEnabled(), roles);
        var accessToken = jwtService.createAccessToken(principal, now);

        return new AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                roles,
                accessToken,
                jwtService.accessTokenExpiresAt(now),
                refresh.rawToken(),
                refresh.expiresAt()
        );
    }

    @Transactional
    public void logout(String refreshTokenRaw, Instant now) {
        String tokenHash = TokenHasher.sha256Hex(refreshTokenRaw);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(rt -> {
            if (rt.getRevokedAt() == null) {
                rt.setRevokedAt(now);
                refreshTokenRepository.save(rt);
            }
        });
    }

    private IssuedRefreshToken issueRefreshToken(java.util.UUID userId, Instant now) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        String raw = generateRefreshToken();
        String hash = TokenHasher.sha256Hex(raw);
        Instant expiresAt = now.plus(props.refreshTokenTtl());

        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setUser(user);
        entity.setTokenHash(hash);
        entity.setExpiresAt(expiresAt);
        refreshTokenRepository.save(entity);

        return new IssuedRefreshToken(raw, expiresAt);
    }

    private IssuedRefreshToken rotateRefreshToken(RefreshTokenEntity current, Instant now) {
        String newRaw = generateRefreshToken();
        String newHash = TokenHasher.sha256Hex(newRaw);
        Instant expiresAt = now.plus(props.refreshTokenTtl());

        current.setRevokedAt(now);
        current.setReplacedByTokenHash(newHash);
        refreshTokenRepository.save(current);

        RefreshTokenEntity next = new RefreshTokenEntity();
        next.setUser(current.getUser());
        next.setTokenHash(newHash);
        next.setExpiresAt(expiresAt);
        refreshTokenRepository.save(next);

        return new IssuedRefreshToken(newRaw, expiresAt);
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private record IssuedRefreshToken(String rawToken, Instant expiresAt) {
    }
}
