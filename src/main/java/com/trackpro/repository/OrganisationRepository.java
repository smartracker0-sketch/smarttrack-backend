package com.trackpro.repository;

import com.trackpro.model.OrganisationEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganisationRepository extends JpaRepository<OrganisationEntity, UUID> {
    boolean existsBySlug(String slug);
    Optional<OrganisationEntity> findBySlug(String slug);
    Page<OrganisationEntity> findByStatus(String status, Pageable pageable);
}
