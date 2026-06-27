package com.trackpro.repository;

import com.trackpro.model.TyreRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface TyreRepository extends JpaRepository<TyreRecord, UUID> {
    Page<TyreRecord> findByOrganisationIdOrderByCreatedAtDesc(UUID orgId, Pageable pageable);
    Page<TyreRecord> findByOrganisationIsNullOrderByCreatedAtDesc(Pageable pageable);
}
