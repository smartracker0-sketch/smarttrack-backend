package com.trackpro.scorecard.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackpro.model.DriverScore;
import com.trackpro.model.UserEntity;
import com.trackpro.repository.DriverScoreRepository;
import com.trackpro.repository.UserRepository;
import com.trackpro.scorecard.DriverScoreService;
import com.trackpro.scorecard.calculator.ScoreBandResolver;
import com.trackpro.scorecard.config.ScorecardConfig;
import com.trackpro.scorecard.dto.DriverScorecardSummary;
import com.trackpro.scorecard.dto.TripScoreBreakdown;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1")
public class ScorecardController {

    private final DriverScoreService driverScoreService;
    private final DriverScoreRepository driverScoreRepository;
    private final UserRepository userRepository;
    private final ScoreBandResolver bandResolver;
    private final ScorecardConfig config;
    private final ObjectMapper objectMapper;

    public ScorecardController(DriverScoreService driverScoreService,
                               DriverScoreRepository driverScoreRepository,
                               UserRepository userRepository,
                               ScoreBandResolver bandResolver,
                               ScorecardConfig config,
                               ObjectMapper objectMapper) {
        this.driverScoreService = driverScoreService;
        this.driverScoreRepository = driverScoreRepository;
        this.userRepository = userRepository;
        this.bandResolver = bandResolver;
        this.config = config;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/drivers/{driverId}/scorecard")
    public DriverScorecardSummary getScorecard(@PathVariable UUID driverId) {
        UserEntity driver = userRepository.findById(driverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<DriverScore> recent = driverScoreRepository.findByDriverIdOrderByCreatedAtDesc(
                driverId, org.springframework.data.domain.PageRequest.of(0, 10));
        List<Integer> last10 = recent.stream().map(DriverScore::getScore).toList();

        List<DriverScore> lastMonth = driverScoreRepository.findByDriverIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                driverId, Instant.now().minus(30, ChronoUnit.DAYS));
        Map<String, Long> violations = lastMonth.stream()
                .flatMap(s -> parseEvents(s).stream())
                .filter(e -> e.points() < 0)
                .collect(Collectors.groupingBy(TripScoreBreakdown.ScoreEvent::type, Collectors.counting()));

        return new DriverScorecardSummary(
                driver.getId(), driver.getDisplayName(),
                driver.getOrganisation() != null ? driver.getOrganisation().getId() : null,
                driver.getScoreTotal(), driver.getScoreBand(),
                bandResolver.colour(ScoreBandResolver.Band.valueOf(driver.getScoreBand())),
                driver.getTotalTripsScored(), 0, last10, violations, driver.getLastScoredAt()
        );
    }

    @GetMapping("/drivers/{driverId}/trips/{tripId}/score")
    public TripScoreBreakdown getTripScore(@PathVariable UUID driverId, @PathVariable UUID tripId) {
        DriverScore ds = driverScoreRepository.findById(tripId)
                .filter(s -> s.getDriver().getId().equals(driverId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<TripScoreBreakdown.ScoreEvent> events = parseEvents(ds);
        ScoreBandResolver.Band band = bandResolver.resolve(ds.getScore());
        return new TripScoreBreakdown(
                ds.getTrip().getId(), ds.getDriver().getId(),
                config.startingScore(), events, ds.isCleanTripBonus() ? config.bonuses().cleanTrip() : 0,
                ds.isStreakBonus() ? config.bonuses().smoothStreakPerTrip() : 0,
                ds.getScore(), ds.getAdjustedScore(), ds.getAdjustmentReason(),
                band.name(), bandResolver.colour(band)
        );
    }

    @GetMapping("/orgs/{orgId}/drivers/leaderboard")
    public Page<DriverScorecardSummary> leaderboard(@PathVariable UUID orgId,
                                                      @PageableDefault(size = 20) Pageable pageable) {
        return driverScoreRepository.findByOrganisationIdOrderByScoreDesc(orgId, pageable)
                .map(ds -> new DriverScorecardSummary(
                        ds.getDriver().getId(), ds.getDriver().getDisplayName(),
                        orgId, ds.getDriver().getScoreTotal(), ds.getDriver().getScoreBand(),
                        bandResolver.colour(ScoreBandResolver.Band.valueOf(ds.getDriver().getScoreBand())),
                        ds.getDriver().getTotalTripsScored(), 0, List.of(), Map.of(),
                        ds.getDriver().getLastScoredAt()
                ));
    }

    @GetMapping("/orgs/{orgId}/scorecard/summary")
    public Map<String, Object> orgSummary(@PathVariable UUID orgId) {
        List<DriverScore> recent = driverScoreRepository.findByOrganisationIdOrderByScoreDesc(
                orgId, org.springframework.data.domain.PageRequest.of(0, 1000)).getContent();
        Map<String, Long> bandDistribution = recent.stream()
                .collect(Collectors.groupingBy(
                        s -> bandResolver.resolve(s.getScore()).name(),
                        Collectors.counting()));
        Map<String, Long> violations = recent.stream()
                .flatMap(s -> parseEvents(s).stream())
                .filter(e -> e.points() < 0)
                .collect(Collectors.groupingBy(TripScoreBreakdown.ScoreEvent::type, Collectors.counting()));
        String topViolation = violations.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse("NONE");
        double avg = recent.isEmpty() ? 0 : recent.stream().mapToInt(DriverScore::getScore).average().orElse(0);
        return Map.of(
                "averageScore", avg,
                "bandDistribution", bandDistribution,
                "mostCommonViolation", topViolation,
                "totalScoredTrips", recent.size()
        );
    }

    @PatchMapping("/drivers/{driverId}/trips/{tripId}/score/override")
    public void overrideScore(@PathVariable UUID driverId, @PathVariable UUID tripId,
                              @RequestBody Map<String, Object> body) {
        Integer adjusted = (Integer) body.get("adjustedScore");
        String reason = (String) body.get("reason");
        driverScoreService.applyOverride(tripId, adjusted, reason);
    }

    private List<TripScoreBreakdown.ScoreEvent> parseEvents(DriverScore ds) {
        if (ds.getBreakdown() == null) return Collections.emptyList();
        try {
            return objectMapper.readValue(ds.getBreakdown(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, TripScoreBreakdown.ScoreEvent.class));
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }
}
