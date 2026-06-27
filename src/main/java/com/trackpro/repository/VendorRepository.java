package com.trackpro.repository;

import com.trackpro.model.Vendor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface VendorRepository extends JpaRepository<Vendor, UUID> {
    Page<Vendor> findByOrganisationIdAndActiveTrueOrderByNameAsc(UUID orgId, Pageable pageable);
    Page<Vendor> findByOrganisationIsNullOrderByNameAsc(Pageable pageable);
}
