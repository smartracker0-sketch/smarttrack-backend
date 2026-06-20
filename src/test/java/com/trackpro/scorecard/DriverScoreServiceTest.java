package com.trackpro.scorecard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class DriverScoreServiceTest {

    private TripScoreSession session;
    private DriverScoreRepository driverScoreRepository;
    private TripRepository tripRepository;
    private UserRepository userRepository;
    private SimpMessagingTemplate ws;
    private DriverScoreService service;

    private final ScorecardConfig config = new ScorecardConfig(100, null, null, null, null, null);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        session = mock(TripScoreSession.class);
        driverScoreRepository = mock(DriverScoreRepository.class);
        tripRepository = mock(TripRepository.class);
        userRepository = mock(UserRepository.class);
        ws = mock(SimpMessagingTemplate.class);
        service = new DriverScoreService(
                session,
                new ScoreCalculator(config),
                new ScoreBandResolver(config),
                new RollingScoreCalculator(config),
                config,
                tripRepository,
                driverScoreRepository,
                userRepository,
                ws,
                objectMapper
        );
    }

    @Test
    void recordEventDeductsPointsForHarshBrake() {
        UUID tripId = UUID.randomUUID();
        TripEntity trip = new TripEntity();
        trip.setId(tripId);
        UserEntity driver = new UserEntity();
        driver.setId(UUID.randomUUID());
        trip.setDriver(driver);
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        TripScoreSession.Session s = new TripScoreSession.Session(tripId, 100, new java.util.ArrayList<>(), false);
        when(session.createOrRead(tripId, 100)).thenReturn(s);

        service.recordEvent(tripId, AlertType.HARSH_BRAKE, AlertSeverity.MEDIUM, false);

        assertEquals(97, s.currentScore());
        assertEquals(1, s.events().size());
        verify(session).write(s);
    }

    @Test
    void recordEventIgnoredForNonDrivingBehaviour() {
        assertEquals(false, service.isDrivingBehaviour(AlertType.LOW_FUEL));
    }

    @Test
    void finalizeTripSavesScoreAndUpdatesDriver() {
        UUID tripId = UUID.randomUUID();
        TripEntity trip = new TripEntity();
        trip.setId(tripId);
        UserEntity driver = new UserEntity();
        driver.setId(UUID.randomUUID());
        trip.setDriver(driver);
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        TripScoreSession.Session s = new TripScoreSession.Session(tripId, 100, new java.util.ArrayList<>(), false);
        when(session.read(tripId)).thenReturn(s);
        when(driverScoreRepository.findByDriverIdOrderByCreatedAtDesc(driver.getId(),
                org.springframework.data.domain.PageRequest.of(0, config.rollingAverage().tripWindow())))
                .thenReturn(java.util.List.of());

        service.finalizeTripScore(tripId);

        ArgumentCaptor<DriverScore> scoreCaptor = ArgumentCaptor.forClass(DriverScore.class);
        verify(driverScoreRepository).save(scoreCaptor.capture());
        DriverScore saved = scoreCaptor.getValue();
        assertEquals(105, saved.getScore()); // clean trip bonus applied
        assertEquals(true, saved.isCleanTripBonus());
        verify(session).delete(tripId);
        verify(userRepository).save(driver);
        assertNotNull(driver.getScoreTotal());
    }
}
