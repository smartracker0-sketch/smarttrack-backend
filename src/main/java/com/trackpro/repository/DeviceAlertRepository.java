package com.trackpro.repository;

import com.trackpro.model.DeviceAlert;
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

    @Modifying
    @Query("update DeviceAlert a set a.acknowledged = true, a.ackAt = current_timestamp, a.ackBy = :user where a.id = :id")
    int acknowledge(@Param("id") UUID id, @Param("user") com.trackpro.model.UserEntity user);

    List<DeviceAlert> findTop2ByDeviceIdAndAlertTypeAndAcknowledgedFalseOrderByAlertTimeDesc(UUID deviceId, com.trackpro.alert.AlertType alertType);
}
