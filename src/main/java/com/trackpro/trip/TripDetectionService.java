package com.trackpro.trip;

import com.trackpro.model.DeviceEntity;
import com.trackpro.model.TripEntity;
import com.trackpro.model.UserEntity;
import com.trackpro.repository.TripRepository;
import com.trackpro.scorecard.DriverScoreService;
import com.trackpro.telemetry.DeviceFrame;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TripDetectionService {

    private static final Logger log = LoggerFactory.getLogger(TripDetectionService.class);
    private static final double MOVING_THRESHOLD_KMH = 5.0;
    private static final Duration STATIONARY_TIMEOUT = Duration.ofMinutes(2);

    private final TripRepository tripRepository;
    private final DriverScoreService driverScoreService;

    public TripDetectionService(TripRepository tripRepository, DriverScoreService driverScoreService) {
        this.tripRepository = tripRepository;
        this.driverScoreService = driverScoreService;
    }

    @Transactional
    public void processTelemetry(DeviceFrame frame, DeviceEntity device) {
        Optional<TripEntity> active = tripRepository
                .findFirstByDeviceIdAndStatusOrderByStartedAtDesc(device.getId(), "IN_PROGRESS");

        boolean moving = frame.speedKph() != null && frame.speedKph() > MOVING_THRESHOLD_KMH;
        boolean ignitionOn = frame.ignition() != null && frame.ignition();

        if (active.isEmpty()) {
            if (ignitionOn && moving) {
                startTrip(device, frame);
            }
            return;
        }

        TripEntity trip = active.get();
        trip.setLastLocationAt(frame.eventTime() != null ? frame.eventTime() : Instant.now());
        tripRepository.save(trip);

        if (!ignitionOn && !moving && isStationaryTimeoutReached(trip)) {
            finalizeTrip(trip);
        }
    }

    private void startTrip(DeviceEntity device, DeviceFrame frame) {
        UserEntity driver = device.getOwner(); // simplified: owner is driver
        if (driver == null) {
            log.warn("Device {} has no assigned driver; cannot start trip", device.getId());
            return;
        }
        TripEntity trip = new TripEntity();
        trip.setDriver(driver);
        trip.setDevice(device);
        trip.setStartedAt(frame.eventTime() != null ? frame.eventTime() : Instant.now());
        trip.setLastLocationAt(trip.getStartedAt());
        trip.setStatus("IN_PROGRESS");
        tripRepository.save(trip);
        driverScoreService.activeTripForDevice(device.getId()); // ensures session is created
    }

    private boolean isStationaryTimeoutReached(TripEntity trip) {
        Instant last = trip.getLastLocationAt();
        if (last == null) return false;
        return Duration.between(last, Instant.now()).compareTo(STATIONARY_TIMEOUT) >= 0;
    }

    private void finalizeTrip(TripEntity trip) {
        driverScoreService.finalizeTripScore(trip.getId());
    }

    @Transactional
    public void forceFinalize(UUID tripId) {
        tripRepository.findById(tripId).ifPresent(this::finalizeTrip);
    }
}
