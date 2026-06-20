package com.trackpro.repository;

import com.trackpro.model.DriverScore;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverScoreRepository extends JpaRepository<DriverScore, UUID> {

    List<DriverScore> findByDriverIdOrderByCreatedAtDesc(UUID driverId, Pageable pageable);

    List<DriverScore> findByDriverIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(UUID driverId, java.time.Instant since);

    Page<DriverScore> findByOrganisationIdOrderByScoreDesc(UUID orgId, Pageable pageable);
}
