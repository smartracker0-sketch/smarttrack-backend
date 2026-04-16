package com.trackpro.repository;

import com.trackpro.model.LocationEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LocationRepository extends JpaRepository<LocationEntity, UUID> {
    Optional<LocationEntity> findTopByDeviceIdOrderByRecordedAtDesc(UUID deviceId);

    @Query("""
            select l
            from LocationEntity l
            where l.device.id = :deviceId
              and l.recordedAt >= :from
              and l.recordedAt <= :to
            order by l.recordedAt asc
            """)
    List<LocationEntity> findHistory(
            @Param("deviceId") UUID deviceId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("""
            select l
            from LocationEntity l
            where l.device.id in :deviceIds
              and l.recordedAt = (
                select max(l2.recordedAt)
                from LocationEntity l2
                where l2.device.id = l.device.id
              )
            """)
    List<LocationEntity> findLatestForDevices(@Param("deviceIds") List<UUID> deviceIds);
}
