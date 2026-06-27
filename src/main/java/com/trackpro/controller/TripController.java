package com.trackpro.controller;

import com.trackpro.model.TripEntity;
import com.trackpro.repository.TripRepository;
import com.trackpro.security.CurrentUser;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips")
public class TripController {

    private final TripRepository tripRepository;

    public TripController(TripRepository tripRepository) {
        this.tripRepository = tripRepository;
    }

    @GetMapping
    public List<TripDto> myTrips() {
        UUID userId = CurrentUser.userId();
        return tripRepository.findByDriverIdAndStatusOrderByStartedAtDesc(userId, "COMPLETED")
                .stream().map(TripController::toDto).toList();
    }

    @GetMapping("/active")
    public List<TripDto> activeTrips() {
        UUID userId = CurrentUser.userId();
        return tripRepository.findByDriverIdAndStatusOrderByStartedAtDesc(userId, "IN_PROGRESS")
                .stream().map(TripController::toDto).toList();
    }

    private static TripDto toDto(TripEntity t) {
        return new TripDto(
                t.getId(),
                t.getDriver().getId(),
                t.getDriver().getDisplayName(),
                t.getDevice().getId(),
                t.getDevice().getName(),
                t.getStartedAt(),
                t.getEndedAt(),
                t.getStatus(),
                t.getCreatedAt());
    }

    public record TripDto(
            UUID id,
            UUID driverId,
            String driverName,
            UUID deviceId,
            String deviceName,
            Instant startedAt,
            Instant endedAt,
            String status,
            Instant createdAt) {}
}
