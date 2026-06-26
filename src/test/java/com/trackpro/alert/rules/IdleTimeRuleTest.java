package com.trackpro.alert.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.trackpro.alert.AlertSeverity;
import com.trackpro.alert.AlertType;
import com.trackpro.alert.config.AlertThresholds;
import com.trackpro.alert.dto.AlertEvent;
import com.trackpro.alert.service.AlertRuleCache;
import com.trackpro.model.DeviceEntity;
import com.trackpro.telemetry.DeviceFrame;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdleTimeRuleTest {

    private final AlertThresholds thresholds = new AlertThresholds(
            new AlertThresholds.Overspeed(100, 3, 10.0, 5),
            new AlertThresholds.HarshDriving(-6.5, 4.5),
            new AlertThresholds.Idle(10, 30, 30),
            new AlertThresholds.DeviceOffline(5, 60),
            new AlertThresholds.Fuel(15, 10.0, 5)
    );

    @Test
    void firesAfterThresholdCrossedWithDebounce() {
        AlertRuleCache cache = mock(AlertRuleCache.class);
        IdleTimeRule rule = new IdleTimeRule(Optional.of(cache), thresholds);
        DeviceEntity device = device();
        Instant now = Instant.now();
        when(cache.getIdleSince(device.getId())).thenReturn(Optional.of(now.minusSeconds(601)));
        when(cache.getLastAlertTime(device.getId(), AlertType.IDLE_EXCEEDED)).thenReturn(Optional.empty());

        DeviceFrame current = frame(now, true, 0.0);
        List<AlertEvent> events = rule.evaluate(current, null, device, mockContext());
        assertThat(events).hasSize(1);
        assertThat(events.get(0).severity()).isEqualTo(AlertSeverity.LOW);
    }

    @Test
    void doesNotFireWithinDebounceWindow() {
        AlertRuleCache cache = mock(AlertRuleCache.class);
        IdleTimeRule rule = new IdleTimeRule(Optional.of(cache), thresholds);
        DeviceEntity device = device();
        Instant now = Instant.now();
        when(cache.getIdleSince(device.getId())).thenReturn(Optional.of(now.minusSeconds(601)));
        when(cache.getLastAlertTime(device.getId(), AlertType.IDLE_EXCEEDED))
                .thenReturn(Optional.of(now.minusSeconds(60)));

        DeviceFrame current = frame(now, true, 0.0);
        List<AlertEvent> events = rule.evaluate(current, null, device, mockContext());
        assertThat(events).isEmpty();
    }

    @Test
    void escalatesAfterEscalationThreshold() {
        AlertRuleCache cache = mock(AlertRuleCache.class);
        IdleTimeRule rule = new IdleTimeRule(Optional.of(cache), thresholds);
        DeviceEntity device = device();
        Instant now = Instant.now();
        when(cache.getIdleSince(device.getId())).thenReturn(Optional.of(now.minusSeconds(1801)));
        when(cache.getLastAlertTime(device.getId(), AlertType.IDLE_EXCEEDED))
                .thenReturn(Optional.of(now.minusSeconds(1800)));

        DeviceFrame current = frame(now, true, 0.0);
        List<AlertEvent> events = rule.evaluate(current, null, device, mockContext());
        assertThat(events).hasSize(1);
        assertThat(events.get(0).severity()).isEqualTo(AlertSeverity.MEDIUM);
    }

    private DeviceEntity device() {
        DeviceEntity d = new DeviceEntity();
        d.setId(UUID.randomUUID());
        d.setImei("123456789012345");
        return d;
    }

    private DeviceFrame frame(Instant ts, boolean ignition, double speed) {
        return DeviceFrame.builder().eventTime(ts).ignition(ignition).speedKph(speed).build();
    }

    private AlertRuleContext mockContext() {
        return mock(AlertRuleContext.class);
    }
}
