package com.trackpro.alert.rules;

import com.trackpro.alert.AlertSeverity;
import com.trackpro.alert.AlertType;
import com.trackpro.alert.dto.AlertEvent;
import com.trackpro.model.DeviceEntity;
import com.trackpro.telemetry.DeviceFrame;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Ignition OFF but speed > 5 km/h sustained for >5 seconds.
 * Potential GPS/sensor fault or vehicle being towed/pushed.
 */
@Component
public class IgnitionAnomalyRule implements AlertRule {

    private static final double MOVING_SPEED_KMH = 5.0;
    private static final double SUSTAINED_SECONDS = 5.0;

    @Override
    public AlertType getType() {
        return AlertType.IGNITION_ANOMALY;
    }

    @Override
    public List<AlertEvent> evaluate(DeviceFrame current, DeviceFrame previous, DeviceEntity device, AlertRuleContext context) {
        if (current == null || current.speedKph() == null || current.ignition() == null) {
            return Collections.emptyList();
        }
        if (current.ignition()) {
            return Collections.emptyList(); // ignition on is normal
        }
        if (current.speedKph() <= MOVING_SPEED_KMH) {
            return Collections.emptyList();
        }

        // Check sustained: previous also ignition OFF and moving
        if (previous != null && previous.ignition() != null && !previous.ignition()
                && previous.speedKph() != null && previous.speedKph() > MOVING_SPEED_KMH
                && current.eventTime() != null && previous.eventTime() != null) {
            double seconds = Duration.between(previous.eventTime(), current.eventTime()).toMillis() / 1000.0;
            if (seconds >= SUSTAINED_SECONDS) {
                Instant now = current.eventTime() != null ? current.eventTime() : Instant.now();
                return List.of(AlertEvent.builder()
                        .deviceId(device.getId())
                        .imei(device.getImei())
                        .orgId(device.getOrganisation() != null ? device.getOrganisation().getId() : null)
                        .alertType(AlertType.IGNITION_ANOMALY)
                        .severity(AlertSeverity.HIGH)
                        .message(String.format("Ignition OFF but moving at %.1f km/h", current.speedKph()))
                        .alertTime(now)
                        .latitude(current.latitude())
                        .longitude(current.longitude())
                        .speedKph(current.speedKph())
                        .build());
            }
        }
        return Collections.emptyList();
    }
}
