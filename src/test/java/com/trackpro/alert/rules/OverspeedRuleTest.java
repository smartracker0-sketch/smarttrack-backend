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
import com.trackpro.model.OrganisationEntity;
import com.trackpro.telemetry.DeviceFrame;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OverspeedRuleTest {

    private AlertRuleCache cache;
    private AlertThresholds thresholds;
    private OverspeedRule rule;
    private DeviceEntity device;

    @BeforeEach
    void setUp() {
        cache = mock(AlertRuleCache.class);
        thresholds = new AlertThresholds(
                new AlertThresholds.Overspeed(100, 3, 10.0, 5),
                new AlertThresholds.HarshDriving(-6.5, 4.5),
                new AlertThresholds.Idle(10, 30, 30),
                new AlertThresholds.DeviceOffline(5, 60),
                new AlertThresholds.Fuel(15, 10.0, 5)
        );
        rule = new OverspeedRule(cache, thresholds);
        device = new DeviceEntity();
        device.setId(UUID.randomUUID());
        device.setImei("123456789012345");
        device.setOrganisation(new OrganisationEntity());
        when(cache.getLastAlertTime(device.getId(), AlertType.OVERSPEED)).thenReturn(Optional.empty());
        when(cache.getCounter(device.getId(), "overspeed")).thenReturn(0L);
    }

    @Test
    void doesNotFireOnSingleGpsJitter() {
        Instant now = Instant.now();
        DeviceFrame current = frame(now, 130.0);
        DeviceFrame previous = frame(now.minusSeconds(1), 95.0);
        when(cache.getCounter(device.getId(), "overspeed")).thenReturn(1L);
        List<AlertEvent> events = rule.evaluate(current, previous, device, mockContext());
        assertThat(events).isEmpty();
    }

    @Test
    void firesAfterThreeSustainedReadings() {
        Instant now = Instant.now();
        DeviceFrame current = frame(now, 130.0);
        DeviceFrame previous = frame(now.minusSeconds(1), 125.0);
        when(cache.getCounter(device.getId(), "overspeed")).thenReturn(2L);
        List<AlertEvent> events = rule.evaluate(current, previous, device, mockContext());
        assertThat(events).hasSize(1);
        assertThat(events.get(0).alertType()).isEqualTo(AlertType.OVERSPEED);
        assertThat(events.get(0).severity()).isEqualTo(AlertSeverity.HIGH);
    }

    @Test
    void severityIsMediumForModerateOverspeed() {
        Instant now = Instant.now();
        DeviceFrame current = frame(now, 105.0);
        DeviceFrame previous = frame(now.minusSeconds(1), 103.0);
        when(cache.getCounter(device.getId(), "overspeed")).thenReturn(2L);
        List<AlertEvent> events = rule.evaluate(current, previous, device, mockContext());
        assertThat(events.get(0).severity()).isEqualTo(AlertSeverity.MEDIUM);
    }

    @Test
    void respectsPerVehicleSpeedLimit() {
        device.setSpeedLimitKmh(80);
        Instant now = Instant.now();
        DeviceFrame current = frame(now, 95.0);
        DeviceFrame previous = frame(now.minusSeconds(1), 92.0);
        when(cache.getCounter(device.getId(), "overspeed")).thenReturn(2L);
        List<AlertEvent> events = rule.evaluate(current, previous, device, mockContext());
        assertThat(events.get(0).message()).contains("limit 80.0");
    }

    private DeviceFrame frame(Instant ts, double speed) {
        return DeviceFrame.builder()
                .eventTime(ts)
                .speedKph(speed)
                .latitude(6.5)
                .longitude(3.4)
                .build();
    }

    private AlertRuleContext mockContext() {
        return mock(AlertRuleContext.class);
    }
}
