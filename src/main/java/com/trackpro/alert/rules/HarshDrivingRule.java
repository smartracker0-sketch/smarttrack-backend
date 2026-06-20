package com.trackpro.alert.rules;

import com.trackpro.alert.AlertSeverity;
import com.trackpro.alert.AlertType;
import com.trackpro.alert.config.AlertThresholds;
import com.trackpro.alert.dto.AlertEvent;
import com.trackpro.model.DeviceEntity;
import com.trackpro.telemetry.DeviceFrame;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Detects harsh braking and harsh acceleration from consecutive telemetry frames.
 * acceleration = (currentSpeed - previousSpeed) / timeDeltaSeconds (m/s²)
 * Harsh braking: deceleration < -6.5 m/s²
 * Harsh acceleration: acceleration > 4.5 m/s²
 */
@Component
public class HarshDrivingRule implements AlertRule {

    private final AlertThresholds thresholds;

    public HarshDrivingRule(AlertThresholds thresholds) {
        this.thresholds = thresholds;
    }

    @Override
    public AlertType getType() {
        return AlertType.HARSH_BRAKE; // representative; evaluate returns both types
    }

    @Override
    public List<AlertEvent> evaluate(DeviceFrame current, DeviceFrame previous, DeviceEntity device, AlertRuleContext context) {
        if (current == null || current.speedKph() == null || previous == null || previous.speedKph() == null) {
            return Collections.emptyList();
        }
        if (current.eventTime() == null || previous.eventTime() == null) {
            return Collections.emptyList();
        }

        double timeDeltaSeconds = Duration.between(previous.eventTime(), current.eventTime()).toMillis() / 1000.0;
        if (timeDeltaSeconds <= 0) {
            return Collections.emptyList();
        }

        double currentMs = current.speedKph() / 3.6;
        double previousMs = previous.speedKph() / 3.6;
        double acceleration = (currentMs - previousMs) / timeDeltaSeconds;

        double brakingThreshold = thresholds.harshDriving().brakingThresholdMs2();
        double accelThreshold = thresholds.harshDriving().accelerationThresholdMs2();

        if (acceleration < brakingThreshold) {
            return List.of(buildEvent(current, device, AlertType.HARSH_BRAKE,
                    String.format("Harsh braking: %.2f m/s²", acceleration)));
        }
        if (acceleration > accelThreshold) {
            return List.of(buildEvent(current, device, AlertType.HARSH_ACCEL,
                    String.format("Harsh acceleration: %.2f m/s²", acceleration)));
        }
        return Collections.emptyList();
    }

    private AlertEvent buildEvent(DeviceFrame current, DeviceEntity device, AlertType type, String message) {
        return AlertEvent.builder()
                .deviceId(device.getId())
                .imei(device.getImei())
                .orgId(device.getOrganisation() != null ? device.getOrganisation().getId() : null)
                .alertType(type)
                .severity(AlertSeverity.MEDIUM)
                .message(message)
                .alertTime(current.eventTime() != null ? current.eventTime() : java.time.Instant.now())
                .latitude(current.latitude())
                .longitude(current.longitude())
                .speedKph(current.speedKph())
                .build();
    }
}
