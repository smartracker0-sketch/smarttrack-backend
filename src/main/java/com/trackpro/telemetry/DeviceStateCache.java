package com.trackpro.telemetry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackpro.dto.telemetry.TelemetryEventDto;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(StringRedisTemplate.class)
public class DeviceStateCache {

    private static final Logger log = LoggerFactory.getLogger(DeviceStateCache.class);
    private static final String KEY_PREFIX = "device:state:";
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @Autowired
    public DeviceStateCache(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public void put(UUID deviceId, TelemetryEventDto dto) {
        try {
            String json = objectMapper.writeValueAsString(dto);
            redis.opsForValue().set(KEY_PREFIX + deviceId, json, TTL);
        } catch (JsonProcessingException e) {
            log.warn("Failed to cache device state for {}: {}", deviceId, e.getMessage());
        }
    }

    public Optional<TelemetryEventDto> get(UUID deviceId) {
        String json = redis.opsForValue().get(KEY_PREFIX + deviceId);
        if (json == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(json, TelemetryEventDto.class));
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize device state for {}: {}", deviceId, e.getMessage());
            return Optional.empty();
        }
    }

    public void evict(UUID deviceId) {
        redis.delete(KEY_PREFIX + deviceId);
    }
}
