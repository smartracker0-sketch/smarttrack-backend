package com.trackpro.scorecard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackpro.alert.AlertSeverity;
import com.trackpro.alert.AlertType;
import com.trackpro.model.DriverScore;
import com.trackpro.model.TripEntity;
import com.trackpro.model.UserEntity;
import com.trackpro.repository.DriverScoreRepository;
import com.trackpro.repository.TripRepository;
import com.trackpro.repository.UserRepository;
import com.trackpro.scorecard.calculator.RollingScoreCalculator;
import com.trackpro.scorecard.calculator.ScoreBandResolver;
import com.trackpro.scorecard.calculator.ScoreCalculator;
import com.trackpro.scorecard.config.ScorecardConfig;
import com.trackpro.scorecard.dto.DriverScoreUpdated;
import com.trackpro.scorecard.dto.TripScoreBreakdown;
import java.math.BigDecimal;
import org.springframework.lang.Nullable;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DriverScoreService {

    private static final Logger log = LoggerFactory.getLogger(DriverScoreService.class);

    @Nullable
    private final TripScoreSession session;
    private final ScoreCalculator scoreCalculator;
    private final ScoreBandResolver bandResolver;
    private final RollingScoreCalculator rollingScoreCalculator;
    private final ScorecardConfig config;
    private final TripRepository tripRepository;
    private final DriverScoreRepository driverScoreRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate ws;
    private final ObjectMapper objectMapper;

    public DriverScoreService(@Nullable TripScoreSession session,
                              ScoreCalculator scoreCalculator,
                              ScoreBandResolver bandResolver,
                              RollingScoreCalculator rollingScoreCalculator,
                              ScorecardConfig config,
                              TripRepository tripRepository,
                              DriverScoreRepository driverScoreRepository,
                              UserRepository userRepository,
                              SimpMessagingTemplate ws,
                              ObjectMapper objectMapper) {
        this.session = session;
        this.scoreCalculator = scoreCalculator;
        this.bandResolver = bandResolver;
        this.rollingScoreCalculator = rollingScoreCalculator;
        this.config = config;
        this.tripRepository = tripRepository;
        this.driverScoreRepository = driverScoreRepository;
        this.userRepository = userRepository;
        this.ws = ws;
        this.objectMapper = objectMapper;
    }

    public boolean isDrivingBehaviour(AlertType type) {
        return type == AlertType.HARSH_BRAKE || type == AlertType.HARSH_ACCEL
                || type == AlertType.OVERSPEED || type == AlertType.IDLE_EXCEEDED
                || type == AlertType.IGNITION_ANOMALY;
    }

    @Transactional
    public void recordEvent(UUID tripId, AlertType type, AlertSeverity severity, boolean restrictedGeofence) {
        if (session == null) {
            log.debug("TripScoreSession unavailable (no Redis); skipping score event");
            return;
        }
        Optional<TripEntity> tripOpt = tripRepository.findById(tripId);
        if (tripOpt.isEmpty()) {
            log.warn("No trip found for score event: {}", tripId);
            return;
        }
        TripEntity trip = tripOpt.get();
        if (trip.getDriver() == null) {
            log.warn("Trip {} has no driver assigned; skipping score event", tripId);
            return;
        }
        TripScoreSession.Session s = session.createOrRead(tripId, config.startingScore());

        int points = scoreCalculator.deductionFor(type, severity, restrictedGeofence);
        if (points == 0) {
            if (type == AlertType.GEOFENCE_ENTER && !restrictedGeofence) {
                return;
            }
        }

        if (type == AlertType.IDLE_EXCEEDED) {
            if (s.idleAlreadyDeducted()) return;
            s.setIdleAlreadyDeducted(true);
        }

        applyDeduction(s, points, type.name(), reason(type, severity, restrictedGeofence));
        session.write(s);
    }

    public void recordNightDriving(UUID tripId, ZonedDateTime ts) {
        if (session == null) return;
        int penalty = scoreCalculator.nightDrivingPenalty(ts);
        if (penalty <= 0) return;
        TripScoreSession.Session s = session.createOrRead(tripId, config.startingScore());
        int minutes = s.nightDrivingMinutesAccrued() + 10;
        int chunks = minutes / 10;
        int previousChunks = s.nightDrivingMinutesAccrued() / 10;
        if (chunks > previousChunks) {
            applyDeduction(s, penalty, "NIGHT_DRIVING", "Night driving penalty");
            s.setNightDrivingMinutesAccrued(minutes);
            session.write(s);
        }
    }

    @Transactional
    public void finalizeTripScore(UUID tripId) {
        if (session == null) {
            log.debug("TripScoreSession unavailable (no Redis); skipping trip finalization");
            return;
        }
        TripScoreSession.Session s = session.read(tripId);
        if (s == null) {
            log.warn("No active score session for trip {}; skipping finalization", tripId);
            return;
        }

        Optional<TripEntity> tripOpt = tripRepository.findById(tripId);
        if (tripOpt.isEmpty()) {
            log.warn("Trip {} not found; cannot finalize score", tripId);
            return;
        }
        TripEntity trip = tripOpt.get();
        UserEntity driver = trip.getDriver();
        if (driver == null) {
            log.warn("Trip {} has no driver; skipping score finalization", tripId);
            return;
        }

        boolean cleanTrip = s.events().isEmpty();
        if (cleanTrip) {
            applyBonus(s, scoreCalculator.cleanTripBonus(), "CLEAN_TRIP", "No violations recorded");
        }

        boolean smoothStreak = hasSmoothStreak(driver.getId());
        if (smoothStreak) {
            applyBonus(s, scoreCalculator.smoothStreakBonus(), "SMOOTH_STREAK", "5+ trips without high severity events");
        }

        int finalScore = clamp(s.currentScore());
        DriverScore score = new DriverScore();
        score.setTrip(trip);
        score.setDriver(driver);
        score.setOrganisation(driver.getOrganisation());
        score.setScore(finalScore);
        score.setCleanTripBonus(cleanTrip);
        score.setStreakBonus(smoothStreak);
        try {
            score.setBreakdown(objectMapper.writeValueAsString(s.events()));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize score breakdown for trip {}: {}", tripId, e.getMessage());
        }
        driverScoreRepository.save(score);

        trip.setEndedAt(Instant.now());
        trip.setStatus("COMPLETED");
        tripRepository.save(trip);

        recalculateRolling(driver);
        session.delete(tripId);

        broadcast(driver, tripId, finalScore);
    }

    @Transactional
    public void abandonTrip(TripEntity trip) {
        trip.setStatus("ABANDONED");
        trip.setEndedAt(Instant.now());
        tripRepository.save(trip);
        if (session != null) session.delete(trip.getId());
    }

    public void applyOverride(UUID tripId, Integer adjustedScore, String reason) {
        DriverScore ds = driverScoreRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("No score for trip: " + tripId));
        ds.setAdjustedScore(adjustedScore);
        ds.setAdjustmentReason(reason);
        driverScoreRepository.save(ds);
    }

    private boolean hasSmoothStreak(UUID driverId) {
        List<DriverScore> recent = driverScoreRepository.findByDriverIdOrderByCreatedAtDesc(driverId,
                PageRequest.of(0, config.bonuses().smoothStreakMinTrips()));
        if (recent.size() < config.bonuses().smoothStreakMinTrips()) return false;
        return recent.stream().noneMatch(this::hadHighSeverity);
    }

    private boolean hadHighSeverity(DriverScore score) {
        try {
            List<TripScoreBreakdown.ScoreEvent> events = objectMapper.readValue(
                    score.getBreakdown(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, TripScoreBreakdown.ScoreEvent.class));
            return events.stream().anyMatch(e -> e.reason() != null && e.reason().contains("HIGH"));
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    private void recalculateRolling(UserEntity driver) {
        List<DriverScore> recent = driverScoreRepository.findByDriverIdOrderByCreatedAtDesc(
                driver.getId(), PageRequest.of(0, config.rollingAverage().tripWindow()));
        List<Integer> scores = recent.stream().map(DriverScore::getScore).toList();
        double overall = rollingScoreCalculator.calculate(scores, recent.size());
        int rounded = (int) Math.round(overall);
        ScoreBandResolver.Band band = bandResolver.resolve(rounded);
        driver.setScoreTotal(BigDecimal.valueOf(rounded).setScale(2, RoundingMode.HALF_UP));
        driver.setScoreBand(band.name());
        driver.setTotalTripsScored(driver.getTotalTripsScored() + 1);
        driver.setLastScoredAt(Instant.now());
        userRepository.save(driver);
    }

    private void applyDeduction(TripScoreSession.Session s, int points, String type, String reason) {
        if (points == 0) return;
        s.events().add(new TripScoreBreakdown.ScoreEvent(type, -points, reason, Instant.now().toString()));
        s.setCurrentScore(clamp(s.currentScore() - points));
    }

    private void applyBonus(TripScoreSession.Session s, int points, String type, String reason) {
        s.events().add(new TripScoreBreakdown.ScoreEvent(type, +points, reason, Instant.now().toString()));
        s.setCurrentScore(clamp(s.currentScore() + points));
    }

    private int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private String reason(AlertType type, AlertSeverity severity, boolean restricted) {
        if (type == AlertType.OVERSPEED) {
            return String.format("Overspeeding (%s severity)", severity);
        }
        if (type == AlertType.GEOFENCE_ENTER) {
            return restricted ? "Restricted geofence entry" : "Geofence entry";
        }
        return type.name();
    }

    private void broadcast(UserEntity driver, UUID tripId, int finalTripScore) {
        if (driver.getOrganisation() == null) return;
        DriverScoreUpdated event = new DriverScoreUpdated(
                driver.getId(), driver.getScoreTotal(), driver.getScoreBand(), tripId, finalTripScore);
        ws.convertAndSend("/topic/org/" + driver.getOrganisation().getId() + "/driver-scores", event);
    }

    public Optional<UUID> activeTripForDevice(UUID deviceId) {
        return tripRepository.findFirstByDeviceIdAndStatusOrderByStartedAtDesc(deviceId, "IN_PROGRESS")
                .map(TripEntity::getId);
    }
}
