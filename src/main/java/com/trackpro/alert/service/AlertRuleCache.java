package com.trackpro.alert.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackpro.dto.telemetry.TelemetryEventDto;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis cache operations used by the alert rule engine.
 * All keys have a TTL so stale data doesn't accumulate.
 */
@Component
@ConditionalOnBean(StringRedisTemplate.class)
public class AlertRuleCache {

    private static final Logger log = LoggerFactory.getLogger(AlertRuleCache.class);
    private static final Duration DEFAULT_TTL = Duration.ofHours(48);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @Autowired
    public AlertRuleCache(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public void setLastSeen(UUID deviceId, Instant ts) {
        redis.opsForValue().set(key("vehicle", deviceId, "lastSeen"), ts.toString(), DEFAULT_TTL);
    }

    public Optional<Instant> getLastSeen(UUID deviceId) {
        String v = redis.opsForValue().get(key("vehicle", deviceId, "lastSeen"));
        if (v == null) return Optional.empty();
        try { return Optional.of(Instant.parse(v)); }
        catch (Exception e) { return Optional.empty(); }
    }

    public void setIdleSince(UUID deviceId, Instant ts) {
        redis.opsForValue().set(key("vehicle", deviceId, "idleSince"), ts.toString(), DEFAULT_TTL);
    }

    public Optional<Instant> getIdleSince(UUID deviceId) {
        String v = redis.opsForValue().get(key("vehicle", deviceId, "idleSince"));
        if (v == null) return Optional.empty();
        try { return Optional.of(Instant.parse(v)); }
        catch (Exception e) { return Optional.empty(); }
    }

    public void clearIdleSince(UUID deviceId) {
        redis.delete(key("vehicle", deviceId, "idleSince"));
    }

    public void setLastFuelLevel(UUID deviceId, double levelPct, Instant ts) {
        try {
            String json = objectMapper.writeValueAsString(new FuelLevel(levelPct, ts));
            redis.opsForValue().set(key("vehicle", deviceId, "lastFuelLevel"), json, DEFAULT_TTL);
        } catch (JsonProcessingException e) {
            log.warn("Failed to cache fuel level for {}: {}", deviceId, e.getMessage());
        }
    }

    public Optional<FuelLevel> getLastFuelLevel(UUID deviceId) {
        String json = redis.opsForValue().get(key("vehicle", deviceId, "lastFuelLevel"));
        if (json == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(json, FuelLevel.class));
        } catch (JsonProcessingException e) {
            log.warn("Failed to read fuel level for {}: {}", deviceId, e.getMessage());
            return Optional.empty();
        }
    }

    public void setOfflineAlertSent(UUID deviceId, boolean sent) {
        redis.opsForValue().set(key("vehicle", deviceId, "offlineAlertSent"), Boolean.toString(sent), DEFAULT_TTL);
    }

    public boolean isOfflineAlertSent(UUID deviceId) {
        return Boolean.parseBoolean(redis.opsForValue().get(key("vehicle", deviceId, "offlineAlertSent")));
    }

    public void setGeofenceInside(UUID geofenceId, UUID deviceId, boolean inside) {
        redis.opsForValue().set(key("geofence", geofenceId, deviceId, "inside"), Boolean.toString(inside), DEFAULT_TTL);
    }

    public Optional<Boolean> getGeofenceInside(UUID geofenceId, UUID deviceId) {
        String v = redis.opsForValue().get(key("geofence", geofenceId, deviceId, "inside"));
        return v == null ? Optional.empty() : Optional.of(Boolean.parseBoolean(v));
    }

    public void setLastAlertTime(UUID deviceId, com.trackpro.alert.AlertType type, Instant ts) {
        redis.opsForValue().set(key("alert", deviceId, type.name(), "lastFiredAt"), ts.toString(), DEFAULT_TTL);
    }

    public Optional<Instant> getLastAlertTime(UUID deviceId, com.trackpro.alert.AlertType type) {
        String v = redis.opsForValue().get(key("alert", deviceId, type.name(), "lastFiredAt"));
        if (v == null) return Optional.empty();
        try { return Optional.of(Instant.parse(v)); }
        catch (Exception e) { return Optional.empty(); }
    }

    public void setCounter(UUID deviceId, String name, long value) {
        redis.opsForValue().set(key("vehicle", deviceId, "counter", name), Long.toString(value), DEFAULT_TTL);
    }

    public long getCounter(UUID deviceId, String name) {
        String v = redis.opsForValue().get(key("vehicle", deviceId, "counter", name));
        return v == null ? 0L : Long.parseLong(v);
    }

    public void setLastLocation(UUID deviceId, TelemetryEventDto dto) {
        try {
            String json = objectMapper.writeValueAsString(dto);
            redis.opsForValue().set(key("vehicle", deviceId, "lastLocation"), json, DEFAULT_TTL);
        } catch (JsonProcessingException e) {
            log.warn("Failed to cache last location for {}: {}", deviceId, e.getMessage());
        }
    }

    public Optional<TelemetryEventDto> getLastLocation(UUID deviceId) {
        String json = redis.opsForValue().get(key("vehicle", deviceId, "lastLocation"));
        if (json == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(json, TelemetryEventDto.class));
        } catch (JsonProcessingException e) {
            log.warn("Failed to read last location for {}: {}", deviceId, e.getMessage());
            return Optional.empty();
        }
    }

    private String key(Object... parts) {
        StringBuilder sb = new StringBuilder();
        for (Object p : parts) {
            if (sb.length() > 0) sb.append(":");
            sb.append(p);
        }
        return sb.toString();
    }

    public record FuelLevel(double levelPct, Instant recordedAt) {}
}
