package com.trackpro.scorecard.scheduler;

import com.trackpro.model.TripEntity;
import com.trackpro.repository.TripRepository;
import com.trackpro.scorecard.DriverScoreService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrphanedTripCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrphanedTripCleanupScheduler.class);
    private static final Duration ORPHAN_TIMEOUT = Duration.ofHours(2);

    private final TripRepository tripRepository;
    private final DriverScoreService driverScoreService;

    public OrphanedTripCleanupScheduler(TripRepository tripRepository, DriverScoreService driverScoreService) {
        this.tripRepository = tripRepository;
        this.driverScoreService = driverScoreService;
    }

    @Scheduled(fixedRate = 60000)
    public void cleanupOrphanedTrips() {
        Instant cutoff = Instant.now().minus(ORPHAN_TIMEOUT);
        List<TripEntity> orphaned = tripRepository.findByStatusAndLastLocationAtBefore("IN_PROGRESS", cutoff);
        for (TripEntity trip : orphaned) {
            log.info("Force-finalising orphaned trip {} (last location at {})", trip.getId(), trip.getLastLocationAt());
            driverScoreService.finalizeTripScore(trip.getId());
        }
    }
}
