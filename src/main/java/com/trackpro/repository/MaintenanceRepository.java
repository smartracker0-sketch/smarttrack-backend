package com.trackpro.repository;

import com.trackpro.model.MaintenanceRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface MaintenanceRepository extends JpaRepository<MaintenanceRecord, UUID> {
    Page<MaintenanceRecord> findByOrganisationIdOrderByDueDateAsc(UUID orgId, Pageable pageable);
    Page<MaintenanceRecord> findByOrganisationIsNullOrderByCreatedAtDesc(Pageable pageable);
}
