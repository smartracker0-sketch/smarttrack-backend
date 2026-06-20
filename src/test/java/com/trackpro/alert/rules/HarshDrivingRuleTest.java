package com.trackpro.alert.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.trackpro.alert.AlertSeverity;
import com.trackpro.alert.AlertType;
import com.trackpro.alert.config.AlertThresholds;
import com.trackpro.alert.dto.AlertEvent;
import com.trackpro.model.DeviceEntity;
import com.trackpro.telemetry.DeviceFrame;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class HarshDrivingRuleTest {

    private final AlertThresholds thresholds = new AlertThresholds(
            new AlertThresholds.Overspeed(100, 3, 10.0, 5),
            new AlertThresholds.HarshDriving(-6.5, 4.5),
            new AlertThresholds.Idle(10, 30, 30),
            new AlertThresholds.DeviceOffline(5, 60),
            new AlertThresholds.Fuel(15, 10.0, 5)
    );
    private final HarshDrivingRule rule = new HarshDrivingRule(thresholds);

    @Test
    void detectsHarshBraking() {
        Instant t1 = Instant.now();
        Instant t2 = t1.plusSeconds(1);
        DeviceFrame previous = frame(t1, 80.0);
        DeviceFrame current = frame(t2, 30.0); // ~50 km/h decel in 1s => ~13.9 m/s²
        DeviceEntity device = device();
        List<AlertEvent> events = rule.evaluate(current, previous, device, mockContext());
        assertThat(events).hasSize(1);
        assertThat(events.get(0).alertType()).isEqualTo(AlertType.HARSH_BRAKE);
        assertThat(events.get(0).severity()).isEqualTo(AlertSeverity.MEDIUM);
    }

    @Test
    void detectsHarshAcceleration() {
        Instant t1 = Instant.now();
        Instant t2 = t1.plusSeconds(2);
        DeviceFrame previous = frame(t1, 10.0);
        DeviceFrame current = frame(t2, 50.0); // 40 km/h in 2s => 5.6 m/s²
        DeviceEntity device = device();
        List<AlertEvent> events = rule.evaluate(current, previous, device, mockContext());
        assertThat(events).hasSize(1);
        assertThat(events.get(0).alertType()).isEqualTo(AlertType.HARSH_ACCEL);
    }

    @Test
    void ignoresNormalDriving() {
        Instant t1 = Instant.now();
        Instant t2 = t1.plusSeconds(5);
        DeviceFrame previous = frame(t1, 50.0);
        DeviceFrame current = frame(t2, 60.0);
        DeviceEntity device = device();
        List<AlertEvent> events = rule.evaluate(current, previous, device, mockContext());
        assertThat(events).isEmpty();
    }

    private DeviceEntity device() {
        DeviceEntity d = new DeviceEntity();
        d.setId(UUID.randomUUID());
        d.setImei("123456789012345");
        return d;
    }

    private DeviceFrame frame(Instant ts, double speed) {
        return DeviceFrame.builder().eventTime(ts).speedKph(speed).build();
    }

    private AlertRuleContext mockContext() {
        return mock(AlertRuleContext.class);
    }
}
