package com.trackpro.repository;

import com.trackpro.model.DashcamEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DashcamEventRepository extends JpaRepository<DashcamEvent, UUID> {

    Page<DashcamEvent> findByDeviceIdOrderByEventTimeDesc(UUID deviceId, Pageable pageable);

    @Query("select d from DashcamEvent d where d.device.id = :deviceId and d.eventTime between :from and :to order by d.eventTime desc")
    List<DashcamEvent> findHistory(@Param("deviceId") UUID deviceId,
                                   @Param("from") Instant from,
                                   @Param("to") Instant to);
}
