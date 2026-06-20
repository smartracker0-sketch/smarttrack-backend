package com.trackpro.repository;

import com.trackpro.model.FuelReading;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FuelReadingRepository extends JpaRepository<FuelReading, UUID> {

    Optional<FuelReading> findTopByDeviceIdOrderByEventTimeDesc(UUID deviceId);

    @Query("select f from FuelReading f where f.device.id = :deviceId and f.eventTime between :from and :to order by f.eventTime desc")
    List<FuelReading> findHistory(@Param("deviceId") UUID deviceId,
                                  @Param("from") Instant from,
                                  @Param("to") Instant to);
}
