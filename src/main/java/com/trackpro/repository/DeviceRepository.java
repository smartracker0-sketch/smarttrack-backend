package com.trackpro.repository;

import com.trackpro.model.DeviceEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRepository extends JpaRepository<DeviceEntity, UUID> {
    boolean existsByImei(String imei);
    Optional<DeviceEntity> findByImei(String imei);
    Page<DeviceEntity> findByOwnerId(UUID ownerId, Pageable pageable);
    Optional<DeviceEntity> findByIdAndOwnerId(UUID id, UUID ownerId);
    Page<DeviceEntity> findByOrganisationId(UUID organisationId, Pageable pageable);
    List<DeviceEntity> findByStatusNot(String status);
    long countByStatus(String status);
    List<DeviceEntity> findByActivationStatusAndActivationAttemptedAtBefore(String activationStatus, Instant before);
    Optional<DeviceEntity> findBySimNumber(String simNumber);
}
