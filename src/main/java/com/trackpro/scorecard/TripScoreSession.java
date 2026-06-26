package com.trackpro.scorecard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackpro.scorecard.dto.TripScoreBreakdown;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis-backed session tracking an active trip's running score.
 * Avoids hitting Postgres on every alert event.
 */
@Component
@ConditionalOnBean(StringRedisTemplate.class)
public class TripScoreSession {

    private static final Logger log = LoggerFactory.getLogger(TripScoreSession.class);
    private static final String KEY_PREFIX = "trip:";
    private static final String SCORE_SUFFIX = ":score";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public TripScoreSession(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public Session read(UUID tripId) {
        String key = key(tripId);
        String current = redis.opsForValue().get(key);
        if (current == null) return null;
        try {
            return objectMapper.readValue(current, Session.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse trip score session {}: {}", tripId, e.getMessage());
            return null;
        }
    }

    public Session createOrRead(UUID tripId, int startingScore) {
        Session existing = read(tripId);
        if (existing != null) return existing;
        Session session = new Session(tripId, startingScore, new ArrayList<>(), false);
        write(session);
        return session;
    }

    public void write(Session session) {
        try {
            String json = objectMapper.writeValueAsString(session);
            redis.opsForValue().set(key(session.tripId()), json);
        } catch (JsonProcessingException e) {
            log.warn("Failed to write trip score session {}: {}", session.tripId(), e.getMessage());
        }
    }

    public void delete(UUID tripId) {
        redis.delete(key(tripId));
    }

    private String key(UUID tripId) {
        return KEY_PREFIX + tripId + SCORE_SUFFIX;
    }

    public static class Session {
        private UUID tripId;
        private int startingScore;
        private int currentScore;
        private List<TripScoreBreakdown.ScoreEvent> events;
        private boolean idleAlreadyDeducted;
        private int nightDrivingMinutesAccrued;

        public Session() {}

        public Session(UUID tripId, int startingScore, List<TripScoreBreakdown.ScoreEvent> events, boolean idleAlreadyDeducted) {
            this.tripId = tripId;
            this.startingScore = startingScore;
            this.currentScore = startingScore;
            this.events = events;
            this.idleAlreadyDeducted = idleAlreadyDeducted;
        }

        public UUID tripId() { return tripId; }
        public void setTripId(UUID tripId) { this.tripId = tripId; }
        public int startingScore() { return startingScore; }
        public void setStartingScore(int startingScore) { this.startingScore = startingScore; }
        public int currentScore() { return currentScore; }
        public void setCurrentScore(int currentScore) { this.currentScore = currentScore; }
        public List<TripScoreBreakdown.ScoreEvent> events() { return events; }
        public void setEvents(List<TripScoreBreakdown.ScoreEvent> events) { this.events = events; }
        public boolean idleAlreadyDeducted() { return idleAlreadyDeducted; }
        public void setIdleAlreadyDeducted(boolean idleAlreadyDeducted) { this.idleAlreadyDeducted = idleAlreadyDeducted; }
        public int nightDrivingMinutesAccrued() { return nightDrivingMinutesAccrued; }
        public void setNightDrivingMinutesAccrued(int nightDrivingMinutesAccrued) { this.nightDrivingMinutesAccrued = nightDrivingMinutesAccrued; }
    }
}
