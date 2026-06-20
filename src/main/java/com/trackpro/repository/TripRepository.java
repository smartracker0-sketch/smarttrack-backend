package com.trackpro.repository;

import com.trackpro.model.TripEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripRepository extends JpaRepository<TripEntity, UUID> {

    Optional<TripEntity> findFirstByDeviceIdAndStatusOrderByStartedAtDesc(UUID deviceId, String status);

    List<TripEntity> findByStatusAndLastLocationAtBefore(String status, Instant lastLocationAt);

    List<TripEntity> findByDriverIdAndStatusOrderByStartedAtDesc(UUID driverId, String status);

    List<TripEntity> findByDriverIdAndStatusAndStartedAtGreaterThanEqual(UUID driverId, String status, Instant since);
}
