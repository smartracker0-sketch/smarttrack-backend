package com.trackpro.repository;

import com.trackpro.model.TelemetryEvent;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TelemetryEventRepository extends JpaRepository<TelemetryEvent, UUID> {

    Optional<TelemetryEvent> findTopByDeviceIdOrderByEventTimeDesc(UUID deviceId);

    Page<TelemetryEvent> findByDeviceIdOrderByEventTimeDesc(UUID deviceId, Pageable pageable);

    @Query("select t from TelemetryEvent t where t.device.id = :deviceId and t.eventTime between :from and :to order by t.eventTime desc")
    List<TelemetryEvent> findHistory(@Param("deviceId") UUID deviceId,
                                     @Param("from") Instant from,
                                     @Param("to") Instant to);

    @Query("""
            select t from TelemetryEvent t
            where t.device.id in :deviceIds
              and t.eventTime = (
                  select max(t2.eventTime) from TelemetryEvent t2 where t2.device.id = t.device.id
              )
            """)
    List<TelemetryEvent> findLatestForDevices(@Param("deviceIds") List<UUID> deviceIds);
}
