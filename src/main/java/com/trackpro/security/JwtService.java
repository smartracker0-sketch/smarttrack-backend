package com.trackpro.security;

import com.trackpro.config.TrackProSecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final TrackProSecurityProperties props;
    private final SecretKey key;

    public JwtService(TrackProSecurityProperties props) {
        this.props = props;
        this.key = buildKey(props.secret());
    }

    public String createAccessToken(TrackProUserPrincipal principal, Instant now) {
        Instant exp = now.plus(props.accessTokenTtl());
        return Jwts.builder()
                .setIssuer(props.issuer())
                .setSubject(principal.userId().toString())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .claim("email", principal.email())
                .claim("roles", principal.roles())
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public Instant accessTokenExpiresAt(Instant now) {
        return now.plus(props.accessTokenTtl());
    }

    public Claims parseAndValidate(String token) {
        return Jwts.parser()
                .requireIssuer(props.issuer())
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID subjectAsUserId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    @SuppressWarnings("unchecked")
    public List<String> roles(Claims claims) {
        Object raw = claims.get("roles");
        if (raw instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    private static SecretKey buildKey(String secret) {
        byte[] bytes;
        if (isBase64(secret)) {
            bytes = Decoders.BASE64.decode(secret);
        } else {
            bytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes");
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    private static boolean isBase64(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return value.matches("^[A-Za-z0-9+/=]+$") && value.length() % 4 == 0;
    }
}
