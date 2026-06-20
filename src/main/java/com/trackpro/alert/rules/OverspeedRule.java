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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Fires when a vehicle exceeds its speed limit for a sustained period.
 * Speed limit: per-vehicle override → org default → global default.
 * Requires 3 consecutive overspeed readings OR speed sustained above limit for ≥10 seconds.
 * Debounce: don't fire again within 5 minutes; instead update duration on the open alert.
 */
@Component
public class OverspeedRule implements AlertRule {

    private static final Logger log = LoggerFactory.getLogger(OverspeedRule.class);

    private final AlertRuleCache cache;
    private final AlertThresholds thresholds;

    public OverspeedRule(AlertRuleCache cache, AlertThresholds thresholds) {
        this.cache = cache;
        this.thresholds = thresholds;
    }

    @Override
    public AlertType getType() {
        return AlertType.OVERSPEED;
    }

    @Override
    public List<AlertEvent> evaluate(DeviceFrame current, DeviceFrame previous, DeviceEntity device, AlertRuleContext context) {
        if (current == null || current.speedKph() == null) return Collections.emptyList();

        UUID deviceId = device.getId();
        double limit = resolveSpeedLimit(device);
        if (current.speedKph() <= limit) {
            return Collections.emptyList();
        }

        Instant now = current.eventTime() != null ? current.eventTime() : Instant.now();

        // Check sustained period
        boolean sustained = isSustained(deviceId, current, previous, limit, now);
        if (!sustained) {
            return Collections.emptyList();
        }

        // Debounce: don't fire again within debounce window
        Optional<Instant> lastFired = cache.getLastAlertTime(deviceId, AlertType.OVERSPEED);
        int debounceMinutes = thresholds.overspeed().debounceMinutes();
        if (lastFired.isPresent() && Duration.between(lastFired.get(), now).toMinutes() < debounceMinutes) {
            return Collections.emptyList(); // duration update handled by AlertService for existing open alert
        }

        AlertSeverity severity = severity(current.speedKph(), limit);
        String message = String.format("Overspeed: %.1f km/h (limit %.1f km/h)", current.speedKph(), limit);

        cache.setLastAlertTime(deviceId, AlertType.OVERSPEED, now);

        return List.of(AlertEvent.builder()
                .deviceId(deviceId)
                .imei(device.getImei())
                .orgId(device.getOrganisation() != null ? device.getOrganisation().getId() : null)
                .alertType(AlertType.OVERSPEED)
                .severity(severity)
                .message(message)
                .alertTime(now)
                .latitude(current.latitude())
                .longitude(current.longitude())
                .speedKph(current.speedKph())
                .build());
    }

    private boolean isSustained(UUID deviceId, DeviceFrame current, DeviceFrame previous, double limit, Instant now) {
        int sustainedReadings = thresholds.overspeed().sustainedReadings();
        double sustainedSeconds = thresholds.overspeed().sustainedSeconds();

        long count = cache.getCounter(deviceId, "overspeed");
        if (previous != null && previous.speedKph() != null && previous.speedKph() > limit) {
            count++;
        } else {
            count = 1;
        }
        cache.setCounter(deviceId, "overspeed", count);

        if (count >= sustainedReadings) {
            return true;
        }

        // Time-based sustained check
        if (previous != null && previous.eventTime() != null && current.eventTime() != null) {
            double secondsAbove = Duration.between(previous.eventTime(), current.eventTime()).toMillis() / 1000.0;
            if (secondsAbove >= sustainedSeconds) {
                return true;
            }
        }

        return false;
    }

    private double resolveSpeedLimit(DeviceEntity device) {
        if (device.getSpeedLimitKmh() != null) return device.getSpeedLimitKmh();
        if (device.getOrganisation() != null && device.getOrganisation().getDefaultSpeedLimitKmh() != null) {
            return device.getOrganisation().getDefaultSpeedLimitKmh();
        }
        return thresholds.overspeed().defaultLimitKmh();
    }

    private AlertSeverity severity(double speed, double limit) {
        double over = speed - limit;
        if (over > 20) return AlertSeverity.HIGH;
        return AlertSeverity.MEDIUM;
    }
}
