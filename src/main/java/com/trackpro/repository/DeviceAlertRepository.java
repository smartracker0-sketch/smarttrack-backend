package com.trackpro.repository;

import com.trackpro.model.DeviceAlert;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeviceAlertRepository extends JpaRepository<DeviceAlert, UUID> {

    Page<DeviceAlert> findByDeviceIdOrderByAlertTimeDesc(UUID deviceId, Pageable pageable);

    List<DeviceAlert> findByDeviceIdAndAcknowledgedFalseOrderByAlertTimeDesc(UUID deviceId);

    @Query("SELECT a FROM DeviceAlert a WHERE a.device.owner.id = :userId ORDER BY a.alertTime DESC")
    List<DeviceAlert> findByDeviceOwnerIdOrderByAlertTimeDesc(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT a FROM DeviceAlert a WHERE a.device.owner.id = :userId OR a.device.organisation.id = :orgId ORDER BY a.alertTime DESC")
    List<DeviceAlert> findByDeviceOwnerOrOrgOrderByAlertTimeDesc(@Param("userId") UUID userId, @Param("orgId") UUID orgId, Pageable pageable);

    long countByAcknowledgedFalse();

    Page<DeviceAlert> findAllByOrderByAlertTimeDesc(Pageable pageable);

    @Modifying
    @Query("update DeviceAlert a set a.acknowledged = true, a.ackAt = :now, a.ackBy = :user where a.id = :id")
    int acknowledge(@Param("id") UUID id, @Param("user") com.trackpro.model.UserEntity user, @Param("now") Instant now);

    List<DeviceAlert> findTop2ByDeviceIdAndAlertTypeAndAcknowledgedFalseOrderByAlertTimeDesc(UUID deviceId, com.trackpro.alert.AlertType alertType);
}
