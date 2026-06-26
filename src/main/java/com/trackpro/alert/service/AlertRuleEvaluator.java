package com.trackpro.alert.service;

import com.trackpro.alert.dto.AlertEvent;
import com.trackpro.alert.rules.AlertRule;
import com.trackpro.alert.rules.AlertRuleContext;
import com.trackpro.dto.telemetry.TelemetryEventDto;
import com.trackpro.model.DeviceEntity;
import com.trackpro.telemetry.DeviceFrame;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Orchestrates all alert rules. Runs asynchronously on the telemetryExecutor
 * so alert evaluation never blocks the TCP/MQTT ingestion thread.
 */
@Component
public class AlertRuleEvaluator {

    private static final Logger log = LoggerFactory.getLogger(AlertRuleEvaluator.class);

    private final List<AlertRule> rules;
    private final AlertRuleContext context;
    private final AlertService alertService;
    @Nullable
    private final AlertRuleCache cache;

    @Autowired
    public AlertRuleEvaluator(List<AlertRule> rules,
                              AlertRuleContext context,
                              AlertService alertService,
                              @Nullable AlertRuleCache cache) {
        this.rules = rules;
        this.context = context;
        this.alertService = alertService;
        this.cache = cache;
    }

    @Async("telemetryExecutor")
    public void evaluate(DeviceFrame payload, DeviceEntity device) {
        DeviceFrame previous = loadPreviousFrame(device.getId());
        try {
            for (AlertRule rule : rules) {
                List<AlertEvent> events = rule.evaluate(payload, previous, device, context);
                for (AlertEvent event : events) {
                    try {
                        alertService.fire(event);
                    } catch (Exception e) {
                        log.error("Failed to fire alert {} for device {}: {}",
                                event.alertType(), device.getId(), e.getMessage(), e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Alert rule evaluation failed for device {}: {}", device.getId(), e.getMessage(), e);
        } finally {
            if (cache != null) {
                cache.setLastSeen(device.getId(), payload.eventTime() != null ? payload.eventTime() : java.time.Instant.now());
                cache.setLastLocation(device.getId(), toDto(payload, device));
            }
        }
    }

    private DeviceFrame loadPreviousFrame(UUID deviceId) {
        if (cache == null) return null;
        return cache.getLastLocation(deviceId)
                .map(this::toFrame)
                .orElse(null);
    }

    private DeviceFrame toFrame(TelemetryEventDto dto) {
        return DeviceFrame.builder()
                .eventTime(dto.eventTime())
                .latitude(dto.latitude())
                .longitude(dto.longitude())
                .speedKph(dto.speedKph())
                .ignition(dto.ignition())
                .build();
    }

    private TelemetryEventDto toDto(DeviceFrame frame, DeviceEntity device) {
        return new TelemetryEventDto(
                null, device.getId(), device.getImei(),
                frame.eventTime() != null ? frame.eventTime() : java.time.Instant.now(),
                java.time.Instant.now(),
                frame.latitude(), frame.longitude(), frame.altitudeM(),
                frame.speedKph(), frame.headingDeg(), frame.accuracyM(),
                frame.satellites(), frame.ignition(), frame.voltageMv(),
                frame.gsmSignal(), frame.odometerM()
        );
    }
}
