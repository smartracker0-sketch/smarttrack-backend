package com.trackpro.telemetry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackpro.config.TelemetryProperties;
import java.time.Instant;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Subscribes to inbound MQTT topics from field devices that publish JSON directly
 * (e.g. MQTT-native sensors, dashcams with WiFi).
 *
 * Supported topics:
 *   trackpro/devices/{imei}/gps       — GPS fix
 *   trackpro/devices/{imei}/fuel      — fuel sensor reading
 *   trackpro/devices/{imei}/dashcam   — dashcam event (future)
 */
@Component
@ConditionalOnBean(IMqttClient.class)
public class MqttInboundListener {

    private static final Logger log = LoggerFactory.getLogger(MqttInboundListener.class);

    private final IMqttClient mqttClient;
    private final TelemetryService telemetryService;
    private final TelemetryProperties props;
    private final ObjectMapper objectMapper;

    @Autowired
    public MqttInboundListener(
            IMqttClient mqttClient,
            TelemetryService telemetryService,
            TelemetryProperties props,
            ObjectMapper objectMapper
    ) {
        this.mqttClient = mqttClient;
        this.telemetryService = telemetryService;
        this.props = props;
        this.objectMapper = objectMapper;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void subscribeOnStartup() {
        String prefix = props.mqtt().topicPrefix();
        String wildcard = prefix + "/+/gps";
        String fuelWildcard = prefix + "/+/fuel";
        try {
            mqttClient.subscribe(wildcard, 1, this::handleGps);
            mqttClient.subscribe(fuelWildcard, 1, this::handleFuel);
            log.info("MQTT subscribed to {} and {}", wildcard, fuelWildcard);
        } catch (MqttException e) {
            log.error("MQTT subscription failed: {}", e.getMessage(), e);
        }
    }

    private void handleGps(String topic, org.eclipse.paho.client.mqttv3.MqttMessage message) {
        try {
            String imei = extractImei(topic);
            if (imei == null) return;
            JsonNode json = objectMapper.readTree(message.getPayload());

            DeviceFrame frame = DeviceFrame.builder()
                    .imei(imei)
                    .eventTime(parseInstant(json, "ts"))
                    .latitude(getDouble(json, "lat"))
                    .longitude(getDouble(json, "lon"))
                    .altitudeM(getDouble(json, "alt"))
                    .speedKph(getDouble(json, "speed"))
                    .headingDeg(getDouble(json, "heading"))
                    .satellites(getInt(json, "sats"))
                    .ignition(getBool(json, "ign"))
                    .voltageMv(getInt(json, "voltage"))
                    .gsmSignal(getInt(json, "gsm"))
                    .rawPayload(new String(message.getPayload()))
                    .build();

            telemetryService.ingestTelemetry(frame);
        } catch (Exception e) {
            log.warn("Failed to process MQTT GPS from topic {}: {}", topic, e.getMessage());
        }
    }

    private void handleFuel(String topic, org.eclipse.paho.client.mqttv3.MqttMessage message) {
        try {
            String imei = extractImei(topic);
            if (imei == null) return;
            JsonNode json = objectMapper.readTree(message.getPayload());
            telemetryService.ingestFuel(
                    imei,
                    getDouble(json, "level_pct"),
                    getDouble(json, "liters"),
                    getDouble(json, "temp_c"),
                    json.has("tank_id") ? json.get("tank_id").asText() : null
            );
        } catch (Exception e) {
            log.warn("Failed to process MQTT fuel from topic {}: {}", topic, e.getMessage());
        }
    }

    private String extractImei(String topic) {
        String[] parts = topic.split("/");
        if (parts.length < 3) return null;
        return parts[parts.length - 2];
    }

    private Instant parseInstant(JsonNode node, String field) {
        if (node.has(field)) {
            try { return Instant.parse(node.get(field).asText()); } catch (Exception ignored) {}
        }
        return Instant.now();
    }

    private Double getDouble(JsonNode node, String field) {
        return node.has(field) ? node.get(field).asDouble() : null;
    }

    private Integer getInt(JsonNode node, String field) {
        return node.has(field) ? node.get(field).asInt() : null;
    }

    private Boolean getBool(JsonNode node, String field) {
        return node.has(field) ? node.get(field).asBoolean() : null;
    }
}
