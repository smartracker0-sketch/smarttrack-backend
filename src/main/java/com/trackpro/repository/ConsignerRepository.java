package com.trackpro.repository;

import com.trackpro.model.Consigner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ConsignerRepository extends JpaRepository<Consigner, UUID> {
    Page<Consigner> findByOrganisationIdAndActiveTrueOrderByNameAsc(UUID orgId, Pageable pageable);
    Page<Consigner> findByOrganisationIsNullOrderByNameAsc(Pageable pageable);
}
