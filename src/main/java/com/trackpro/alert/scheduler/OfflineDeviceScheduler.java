package com.trackpro.alert.scheduler;

import com.trackpro.alert.AlertSeverity;
import com.trackpro.alert.AlertType;
import com.trackpro.alert.config.AlertThresholds;
import com.trackpro.alert.dto.AlertEvent;
import com.trackpro.alert.service.AlertRuleCache;
import com.trackpro.alert.service.AlertService;
import com.trackpro.model.DeviceEntity;
import com.trackpro.repository.DeviceRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job that checks whether active vehicles have stopped sending telemetry.
 * Fires a DEVICE_OFFLINE alert once per offline episode.
 */
@Component
public class OfflineDeviceScheduler {

    private static final Logger log = LoggerFactory.getLogger(OfflineDeviceScheduler.class);

    private final DeviceRepository deviceRepository;
    private final Optional<AlertRuleCache> cache;
    private final AlertService alertService;
    private final AlertThresholds thresholds;

    public OfflineDeviceScheduler(DeviceRepository deviceRepository,
                                  Optional<AlertRuleCache> cache,
                                  AlertService alertService,
                                  AlertThresholds thresholds) {
        this.deviceRepository = deviceRepository;
        this.cache = cache;
        this.alertService = alertService;
        this.thresholds = thresholds;
    }

    @Scheduled(fixedRate = 60000)
    public void checkOfflineDevices() {
        if (cache.isEmpty()) return;
        AlertRuleCache c = cache.get();
        Instant now = Instant.now();
        int thresholdMinutes = thresholds.deviceOffline().thresholdMinutes();
        int escalationMinutes = thresholds.deviceOffline().escalationMinutes();

        List<DeviceEntity> activeDevices = deviceRepository.findByStatusNot("Offline");
        for (DeviceEntity device : activeDevices) {
            UUID deviceId = device.getId();
            Instant lastSeen = c.getLastSeen(deviceId).orElse(device.getCreatedAt());
            long offlineMinutes = Duration.between(lastSeen, now).toMinutes();

            if (offlineMinutes > thresholdMinutes) {
                if (!c.isOfflineAlertSent(deviceId)) {
                    AlertSeverity severity = offlineMinutes > escalationMinutes ? AlertSeverity.HIGH : AlertSeverity.MEDIUM;
                    AlertEvent event = AlertEvent.builder()
                            .deviceId(deviceId)
                            .imei(device.getImei())
                            .orgId(device.getOrganisation() != null ? device.getOrganisation().getId() : null)
                            .alertType(AlertType.DEVICE_OFFLINE)
                            .severity(severity)
                            .message(String.format("Device offline for %d minutes", offlineMinutes))
                            .alertTime(now)
                            .durationSeconds(Duration.between(lastSeen, now).getSeconds())
                            .build();
                    alertService.fire(event);
                    c.setOfflineAlertSent(deviceId, true);
                }
            } else {
                if (c.isOfflineAlertSent(deviceId)) {
                    c.setOfflineAlertSent(deviceId, false);
                }
            }
        }
    }
}
