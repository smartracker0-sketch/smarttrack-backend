package com.trackpro.alert.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackpro.alert.AlertSeverity;
import com.trackpro.alert.dto.AlertEvent;
import com.trackpro.config.TelemetryProperties;
import com.trackpro.dto.telemetry.DeviceAlertDto;
import com.trackpro.model.DeviceAlert;
import com.trackpro.model.DeviceEntity;
import com.trackpro.model.GeofenceEntity;
import com.trackpro.repository.DeviceAlertRepository;
import com.trackpro.repository.DeviceRepository;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final DeviceAlertRepository alertRepository;
    private final DeviceRepository deviceRepository;
    private final SimpMessagingTemplate ws;
    private final TelemetryProperties props;
    private final ObjectMapper objectMapper;
    private final Optional<IMqttClient> mqttClient;

    @Autowired
    public AlertService(
            DeviceAlertRepository alertRepository,
            DeviceRepository deviceRepository,
            SimpMessagingTemplate ws,
            TelemetryProperties props,
            ObjectMapper objectMapper,
            Optional<IMqttClient> mqttClient
    ) {
        this.alertRepository = alertRepository;
        this.deviceRepository = deviceRepository;
        this.ws = ws;
        this.props = props;
        this.objectMapper = objectMapper;
        this.mqttClient = mqttClient;
    }

    @Transactional
    public void fire(AlertEvent event) {
        DeviceEntity device = deviceRepository.findById(event.deviceId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown device: " + event.deviceId()));

        // For overspeed during debounce: update the open alert's duration instead of creating a new one
        if (event.alertType() == com.trackpro.alert.AlertType.OVERSPEED) {
            Optional<DeviceAlert> openAlert = alertRepository
                    .findTop2ByDeviceIdAndAlertTypeAndAcknowledgedFalseOrderByAlertTimeDesc(device.getId(), event.alertType())
                    .stream().findFirst();
            if (openAlert.isPresent()) {
                DeviceAlert alert = openAlert.get();
                long seconds = Duration.between(alert.getAlertTime(), event.alertTime()).getSeconds();
                alert.setDurationSeconds(Math.max(0, seconds));
                alertRepository.save(alert);
                broadcast(alert);
                return;
            }
        }

        DeviceAlert alert = toEntity(event, device);
        alertRepository.save(alert);
        broadcast(alert);

        if (event.severity() == AlertSeverity.HIGH || event.severity() == AlertSeverity.CRITICAL) {
            sendHighSeverityNotification(event);
        }
    }

    private void broadcast(DeviceAlert alert) {
        DeviceAlertDto dto = toDto(alert);
        ws.convertAndSend("/topic/devices/" + alert.getDevice().getId() + "/alerts", dto);
        ws.convertAndSend("/topic/alerts", dto);
        publishToMqtt(alert.getDevice().getId(), dto);
    }

    private void publishToMqtt(UUID deviceId, DeviceAlertDto dto) {
        if (!props.mqtt().enabled()) return;
        mqttClient.ifPresent(client -> {
            try {
                String topic = props.mqtt().topicPrefix() + "/" + deviceId + "/alert";
                String json = objectMapper.writeValueAsString(dto);
                MqttMessage msg = new MqttMessage(json.getBytes());
                msg.setQos(1);
                client.publish(topic, msg);
            } catch (MqttException | JsonProcessingException e) {
                log.warn("MQTT alert publish failed for device {}: {}", deviceId, e.getMessage());
            }
        });
    }

    private void sendHighSeverityNotification(AlertEvent event) {
        // Placeholder for Termii / Africa's Talking SMS and SendGrid/SES email integration
        log.info("HIGH/CRITICAL alert notification would be sent: {} for device {}",
                event.alertType(), event.deviceId());
    }

    private DeviceAlert toEntity(AlertEvent event, DeviceEntity device) {
        DeviceAlert alert = new DeviceAlert();
        alert.setDevice(device);
        alert.setAlertTime(event.alertTime());
        alert.setAlertType(event.alertType());
        alert.setSeverity(event.severity());
        alert.setMessage(event.message());
        alert.setLatitude(event.latitude());
        alert.setLongitude(event.longitude());
        alert.setSpeedKph(event.speedKph());
        alert.setDurationSeconds(event.durationSeconds());
        if (event.relatedGeofenceId() != null) {
            GeofenceEntity geofence = new GeofenceEntity();
            geofence.setId(event.relatedGeofenceId());
            alert.setRelatedGeofence(geofence);
        }
        return alert;
    }

    private DeviceAlertDto toDto(DeviceAlert a) {
        return new DeviceAlertDto(
                a.getId(), a.getDevice().getId(), a.getAlertTime(), a.getReceivedAt(),
                a.getAlertType(), a.getSeverity(), a.getMessage(),
                a.isAcknowledged(), a.getAckAt(), a.getLatitude(), a.getLongitude(),
                a.getSpeedKph(), a.getDurationSeconds(),
                a.getRelatedGeofence() != null ? a.getRelatedGeofence().getId() : null,
                a.getRelatedGeofence() != null ? a.getRelatedGeofence().getName() : null
        );
    }
}
