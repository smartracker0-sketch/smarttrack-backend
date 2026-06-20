package com.trackpro.telemetry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackpro.config.TelemetryProperties;
import com.trackpro.dto.telemetry.DeviceAlertDto;
import com.trackpro.dto.telemetry.FuelReadingDto;
import com.trackpro.dto.telemetry.TelemetryEventDto;
import com.trackpro.model.DeviceAlert;
import com.trackpro.model.DeviceEntity;
import com.trackpro.model.FuelReading;
import com.trackpro.model.TelemetryEvent;
import com.trackpro.repository.DeviceAlertRepository;
import com.trackpro.repository.DeviceRepository;
import com.trackpro.repository.FuelReadingRepository;
import com.trackpro.repository.TelemetryEventRepository;
import jakarta.transaction.Transactional;
import java.time.Instant;
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

@Service
public class TelemetryService {

    private static final Logger log = LoggerFactory.getLogger(TelemetryService.class);

    private final DeviceRepository deviceRepository;
    private final TelemetryEventRepository telemetryRepository;
    private final FuelReadingRepository fuelRepository;
    private final DeviceAlertRepository alertRepository;
    private final SimpMessagingTemplate ws;
    private final TelemetryProperties props;
    private final ObjectMapper objectMapper;
    private final Optional<DeviceStateCache> stateCache;
    private final Optional<IMqttClient> mqttClient;

    @Autowired
    public TelemetryService(
            DeviceRepository deviceRepository,
            TelemetryEventRepository telemetryRepository,
            FuelReadingRepository fuelRepository,
            DeviceAlertRepository alertRepository,
            SimpMessagingTemplate ws,
            TelemetryProperties props,
            ObjectMapper objectMapper,
            Optional<DeviceStateCache> stateCache,
            Optional<IMqttClient> mqttClient
    ) {
        this.deviceRepository = deviceRepository;
        this.telemetryRepository = telemetryRepository;
        this.fuelRepository = fuelRepository;
        this.alertRepository = alertRepository;
        this.ws = ws;
        this.props = props;
        this.objectMapper = objectMapper;
        this.stateCache = stateCache;
        this.mqttClient = mqttClient;
    }

    @Transactional
    public TelemetryEventDto ingestTelemetry(DeviceFrame frame) {
        DeviceEntity device = deviceRepository.findByImei(frame.imei())
                .orElseGet(() -> {
                    log.warn("Unknown IMEI {}, creating unassigned device record", frame.imei());
                    DeviceEntity d = new DeviceEntity();
                    d.setImei(frame.imei());
                    d.setName(frame.imei());
                    return deviceRepository.save(d);
                });

        TelemetryEvent event = new TelemetryEvent();
        event.setDevice(device);
        event.setEventTime(frame.eventTime() != null ? frame.eventTime() : Instant.now());
        event.setReceivedAt(Instant.now());
        event.setLatitude(frame.latitude());
        event.setLongitude(frame.longitude());
        event.setAltitudeM(frame.altitudeM());
        event.setSpeedKph(frame.speedKph());
        event.setHeadingDeg(frame.headingDeg());
        event.setAccuracyM(frame.accuracyM());
        event.setSatellites(frame.satellites());
        event.setIgnition(frame.ignition());
        event.setVoltageMv(frame.voltageMv());
        event.setGsmSignal(frame.gsmSignal());
        event.setOdometerM(frame.odometerM());
        event.setRawPayload(frame.rawPayload());
        telemetryRepository.save(event);

        device.setStatus("Online");
        deviceRepository.save(device);

        TelemetryEventDto dto = toDto(event);

        stateCache.ifPresent(c -> c.put(device.getId(), dto));

        publishToMqtt(device.getId(), "telemetry", dto);

        ws.convertAndSend("/topic/devices/" + device.getId() + "/telemetry", dto);
        ws.convertAndSend("/topic/telemetry", dto);

        checkAlerts(device, frame);

        return dto;
    }

    @Transactional
    public FuelReadingDto ingestFuel(String imei, Double levelPct, Double liters, Double tempC, String tankId) {
        DeviceEntity device = deviceRepository.findByImei(imei)
                .orElseThrow(() -> new IllegalArgumentException("Unknown IMEI: " + imei));

        FuelReading reading = new FuelReading();
        reading.setDevice(device);
        reading.setEventTime(Instant.now());
        reading.setFuelLevelPct(levelPct);
        reading.setFuelLiters(liters);
        reading.setTemperatureC(tempC);
        reading.setTankId(tankId);
        fuelRepository.save(reading);

        FuelReadingDto dto = new FuelReadingDto(reading.getId(), device.getId(),
                reading.getEventTime(), reading.getReceivedAt(),
                levelPct, liters, tempC, tankId);

        ws.convertAndSend("/topic/devices/" + device.getId() + "/fuel", dto);
        publishToMqtt(device.getId(), "fuel", dto);
        return dto;
    }

    private void checkAlerts(DeviceEntity device, DeviceFrame frame) {
        if (frame.speedKph() != null && frame.speedKph() > 120.0) {
            raiseAlert(device, "SPEEDING", "CRITICAL",
                    String.format("Speed %.1f km/h exceeds limit", frame.speedKph()),
                    frame.latitude(), frame.longitude());
        }
        if (frame.voltageMv() != null && frame.voltageMv() < 11500) {
            raiseAlert(device, "LOW_BATTERY", "WARNING",
                    String.format("Battery voltage low: %d mV", frame.voltageMv()),
                    frame.latitude(), frame.longitude());
        }
    }

    private void raiseAlert(DeviceEntity device, String type, String severity,
                             String message, Double lat, Double lon) {
        DeviceAlert alert = new DeviceAlert();
        alert.setDevice(device);
        alert.setAlertTime(Instant.now());
        alert.setAlertType(type);
        alert.setSeverity(severity);
        alert.setMessage(message);
        alert.setLatitude(lat);
        alert.setLongitude(lon);
        alertRepository.save(alert);

        DeviceAlertDto dto = toAlertDto(alert);
        ws.convertAndSend("/topic/devices/" + device.getId() + "/alerts", dto);
        ws.convertAndSend("/topic/alerts", dto);
        publishToMqtt(device.getId(), "alert", dto);
    }

    private void publishToMqtt(UUID deviceId, String subTopic, Object payload) {
        if (!props.mqtt().enabled()) return;
        mqttClient.ifPresent(client -> {
            try {
                String topic = props.mqtt().topicPrefix() + "/" + deviceId + "/" + subTopic;
                String json = objectMapper.writeValueAsString(payload);
                MqttMessage msg = new MqttMessage(json.getBytes());
                msg.setQos(1);
                msg.setRetained(subTopic.equals("telemetry"));
                client.publish(topic, msg);
            } catch (MqttException | JsonProcessingException e) {
                log.warn("MQTT publish failed for device {}: {}", deviceId, e.getMessage());
            }
        });
    }

    public static TelemetryEventDto toDto(TelemetryEvent e) {
        return new TelemetryEventDto(
                e.getId(), e.getDevice().getId(), e.getDevice().getImei(),
                e.getEventTime(), e.getReceivedAt(),
                e.getLatitude(), e.getLongitude(), e.getAltitudeM(),
                e.getSpeedKph(), e.getHeadingDeg(), e.getAccuracyM(),
                e.getSatellites(), e.getIgnition(), e.getVoltageMv(),
                e.getGsmSignal(), e.getOdometerM()
        );
    }

    public static DeviceAlertDto toAlertDto(DeviceAlert a) {
        return new DeviceAlertDto(
                a.getId(), a.getDevice().getId(), a.getAlertTime(), a.getReceivedAt(),
                a.getAlertType(), a.getSeverity(), a.getMessage(),
                a.isAcknowledged(), a.getAckAt(), a.getLatitude(), a.getLongitude()
        );
    }
}
