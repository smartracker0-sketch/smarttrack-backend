package com.trackpro.repository;

import com.trackpro.model.ElockDevice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ElockRepository extends JpaRepository<ElockDevice, UUID> {
    Page<ElockDevice> findByOrganisationIdOrderByCreatedAtDesc(UUID orgId, Pageable pageable);
    Page<ElockDevice> findByOrganisationIsNullOrderByCreatedAtDesc(Pageable pageable);
}
