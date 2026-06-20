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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Low fuel alert when fuel level < 15%.
 * Fuel drainage alert when fuel drops >10% within 5 minutes while stationary.
 */
@Component
public class FuelAnomalyRule implements AlertRule {

    private final AlertRuleCache cache;
    private final AlertThresholds thresholds;

    public FuelAnomalyRule(AlertRuleCache cache, AlertThresholds thresholds) {
        this.cache = cache;
        this.thresholds = thresholds;
    }

    @Override
    public AlertType getType() {
        return AlertType.LOW_FUEL;
    }

    @Override
    public List<AlertEvent> evaluate(DeviceFrame current, DeviceFrame previous, DeviceEntity device, AlertRuleContext context) {
        if (current == null || current.fuelLevelPct() == null) {
            return Collections.emptyList();
        }

        UUID deviceId = device.getId();
        double currentLevel = current.fuelLevelPct();
        Instant now = current.eventTime() != null ? current.eventTime() : Instant.now();
        List<AlertEvent> events = new ArrayList<>();

        // Low fuel
        if (currentLevel < thresholds.fuel().lowFuelPercent()) {
            events.add(AlertEvent.builder()
                    .deviceId(deviceId)
                    .imei(device.getImei())
                    .orgId(device.getOrganisation() != null ? device.getOrganisation().getId() : null)
                    .alertType(AlertType.LOW_FUEL)
                    .severity(AlertSeverity.LOW)
                    .message(String.format("Low fuel: %.1f%%", currentLevel))
                    .alertTime(now)
                    .latitude(current.latitude())
                    .longitude(current.longitude())
                    .build());
        }

        // Drainage detection
        Optional<AlertRuleCache.FuelLevel> lastOpt = cache.getLastFuelLevel(deviceId);
        if (lastOpt.isPresent()) {
            AlertRuleCache.FuelLevel last = lastOpt.get();
            double drop = last.levelPct() - currentLevel;
            long minutesSinceLast = Duration.between(last.recordedAt(), now).toMinutes();
            boolean stationary = isStationary(current);
            if (drop > thresholds.fuel().drainageThresholdPercent()
                    && minutesSinceLast <= thresholds.fuel().drainageWindowMinutes()
                    && stationary) {
                events.add(AlertEvent.builder()
                        .deviceId(deviceId)
                        .imei(device.getImei())
                        .orgId(device.getOrganisation() != null ? device.getOrganisation().getId() : null)
                        .alertType(AlertType.FUEL_DRAINAGE)
                        .severity(AlertSeverity.HIGH)
                        .message(String.format("Fuel dropped %.1f%% while stationary (%.1f%% → %.1f%%)",
                                drop, last.levelPct(), currentLevel))
                        .alertTime(now)
                        .latitude(current.latitude())
                        .longitude(current.longitude())
                        .build());
            }
        }

        cache.setLastFuelLevel(deviceId, currentLevel, now);
        return events;
    }

    private boolean isStationary(DeviceFrame current) {
        boolean ignitionOff = current.ignition() != null && !current.ignition();
        boolean notMoving = current.speedKph() == null || current.speedKph() < 1.0;
        return ignitionOff || notMoving;
    }
}
