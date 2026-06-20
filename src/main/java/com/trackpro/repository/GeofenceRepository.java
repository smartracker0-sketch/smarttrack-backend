package com.trackpro.repository;

import com.trackpro.model.GeofenceEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GeofenceRepository extends JpaRepository<GeofenceEntity, UUID> {

    List<GeofenceEntity> findByOrganisationIdAndActiveTrue(UUID organisationId);
}
