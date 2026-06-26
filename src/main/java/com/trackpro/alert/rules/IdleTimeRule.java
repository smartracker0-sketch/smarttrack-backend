package com.trackpro.alert.rules;

import com.trackpro.alert.AlertSeverity;
import com.trackpro.alert.AlertType;
import com.trackpro.alert.config.AlertThresholds;
import com.trackpro.alert.dto.AlertEvent;
import com.trackpro.alert.service.AlertRuleCache;
import com.trackpro.model.DeviceEntity;
import com.trackpro.telemetry.DeviceFrame;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Idle = ignition ON but speed < 2 km/h (accounting for GPS drift).
 * Fires when idle exceeds threshold; escalates after escalation threshold.
 * Debounce: fire once when threshold crossed, then again every debounce minutes.
 */
@Component
public class IdleTimeRule implements AlertRule {

    private static final double IDLE_SPEED_KMH = 2.0;

    private final Optional<AlertRuleCache> cache;
    private final AlertThresholds thresholds;

    public IdleTimeRule(Optional<AlertRuleCache> cache, AlertThresholds thresholds) {
        this.cache = cache;
        this.thresholds = thresholds;
    }

    @Override
    public AlertType getType() {
        return AlertType.IDLE_EXCEEDED;
    }

    @Override
    public List<AlertEvent> evaluate(DeviceFrame current, DeviceFrame previous, DeviceEntity device, AlertRuleContext context) {
        if (current == null) return Collections.emptyList();

        UUID deviceId = device.getId();
        boolean ignitionOn = current.ignition() != null && current.ignition();
        double speed = current.speedKph() != null ? current.speedKph() : 0.0;
        boolean isIdle = ignitionOn && speed < IDLE_SPEED_KMH;
        Instant now = current.eventTime() != null ? current.eventTime() : Instant.now();

        int thresholdMinutes = resolveThreshold(device);
        int escalationMinutes = resolveEscalation(device);
        int debounceMinutes = thresholds.idle().debounceMinutes();

        if (cache.isEmpty()) return Collections.emptyList();
        AlertRuleCache c = cache.get();
        Optional<Instant> idleSince = c.getIdleSince(deviceId);
        Optional<Instant> lastFired = c.getLastAlertTime(deviceId, AlertType.IDLE_EXCEEDED);

        if (isIdle) {
            Instant start = idleSince.orElse(now);
            if (idleSince.isEmpty()) {
                c.setIdleSince(deviceId, start);
            }
            long idleMinutes = Duration.between(start, now).toMinutes();

            if (idleMinutes >= thresholdMinutes) {
                // Debounce check
                if (lastFired.isPresent() && Duration.between(lastFired.get(), now).toMinutes() < debounceMinutes) {
                    return Collections.emptyList();
                }
                c.setLastAlertTime(deviceId, AlertType.IDLE_EXCEEDED, now);
                AlertSeverity severity = idleMinutes >= escalationMinutes ? AlertSeverity.MEDIUM : AlertSeverity.LOW;
                return List.of(AlertEvent.builder()
                        .deviceId(deviceId)
                        .imei(device.getImei())
                        .orgId(device.getOrganisation() != null ? device.getOrganisation().getId() : null)
                        .alertType(AlertType.IDLE_EXCEEDED)
                        .severity(severity)
                        .message(String.format("Idle for %d minutes", idleMinutes))
                        .alertTime(now)
                        .latitude(current.latitude())
                        .longitude(current.longitude())
                        .durationSeconds(Duration.between(start, now).getSeconds())
                        .build());
            }
        } else {
            c.clearIdleSince(deviceId);
        }
        return Collections.emptyList();
    }

    private int resolveThreshold(DeviceEntity device) {
        if (device.getOrganisation() != null && device.getOrganisation().getIdleThresholdMinutes() != null) {
            return device.getOrganisation().getIdleThresholdMinutes();
        }
        return thresholds.idle().thresholdMinutes();
    }

    private int resolveEscalation(DeviceEntity device) {
        if (device.getOrganisation() != null && device.getOrganisation().getIdleEscalationMinutes() != null) {
            return device.getOrganisation().getIdleEscalationMinutes();
        }
        return thresholds.idle().escalationMinutes();
    }
}
