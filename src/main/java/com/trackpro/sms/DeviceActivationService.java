package com.trackpro.sms;

import com.trackpro.config.SmsProperties;
import com.trackpro.model.DeviceEntity;
import com.trackpro.repository.DeviceRepository;
import com.trackpro.sms.dto.DeviceActivationEvent;
import com.trackpro.sms.dto.DeviceStatusReply;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeviceActivationService {

    private static final Logger log = LoggerFactory.getLogger(DeviceActivationService.class);
    private static final String REDIS_PREFIX = "device:activation:pending:";

    private final SmsService smsService;
    private final DeviceRepository deviceRepository;
    private final StringRedisTemplate redis;
    private final SimpMessagingTemplate ws;
    private final InboundSmsParser parser;
    private final SmsProperties props;
    private final SmsPhoneNumberNormalizer phoneNumberNormalizer;
    private final DeviceCommandBuilder commandBuilder;

    public DeviceActivationService(SmsService smsService,
                                   DeviceRepository deviceRepository,
                                   StringRedisTemplate redis,
                                   SimpMessagingTemplate ws,
                                   InboundSmsParser parser,
                                   SmsProperties props,
                                   SmsPhoneNumberNormalizer phoneNumberNormalizer,
                                   DeviceCommandBuilder commandBuilder) {
        this.smsService = smsService;
        this.deviceRepository = deviceRepository;
        this.redis = redis;
        this.ws = ws;
        this.parser = parser;
        this.props = props;
        this.phoneNumberNormalizer = phoneNumberNormalizer;
        this.commandBuilder = commandBuilder;
    }

    @Async("smsExecutor")
    public void initiateCheck(DeviceEntity device) {
        if (device.getSimNumber() == null || device.getSimNumber().isBlank()) {
            log.warn("Activation skipped for IMEI {} — no SIM number recorded", device.getImei());
            return;
        }

        try {
            for (String command : commandBuilder.buildActivationCommands(device)) {
                smsService.send(device.getSimNumber(), command, device.getImei());
                Thread.sleep(5_000);
            }

            if (props.getServer().getHost() != null && !props.getServer().getHost().isBlank()) {
                device.setServerConfigured(true);
            }
            if (device.getSimApn() != null && !device.getSimApn().isBlank()) {
                device.setApnConfigured(true);
            }

            device.setActivationStatus(DeviceActivationStatus.PENDING.name());
            device.setActivationAttemptedAt(Instant.now());
            deviceRepository.save(device);

            String normalisedSim = phoneNumberNormalizer.normalise(device.getSimNumber());
            redis.opsForValue().set(
                    REDIS_PREFIX + normalisedSim,
                    device.getImei(),
                    Duration.ofMinutes(props.getActivation().getTimeoutMinutes())
            );

            log.info("Activation check initiated for IMEI {} SIM {}", device.getImei(), device.getSimNumber());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Activation check interrupted for IMEI {}", device.getImei());
        } catch (Exception e) {
            log.error("Activation check failed for IMEI {}: {}", device.getImei(), e.getMessage(), e);
        }
    }

    @Transactional
    public void handleActivationReply(String imei, String rawReplyText) {
        DeviceEntity device = deviceRepository.findByImei(imei)
                .orElseThrow(() -> new IllegalArgumentException("Device not found: " + imei));

        DeviceStatusReply reply = parser.parse(rawReplyText, device.getManufacturer());

        boolean gpsOk    = Boolean.TRUE.equals(reply.gpsFixed()) && reply.latitude() != null;
        boolean signalOk = reply.gsmSignalBars() != null && reply.gsmSignalBars() >= 2;
        boolean healthy  = gpsOk && signalOk;

        device.setLastSmsReply(rawReplyText);

        if (healthy) {
            device.setActivationStatus(DeviceActivationStatus.ACTIVE.name());
            device.setActivationConfirmedAt(Instant.now());
            deviceRepository.save(device);

            broadcastActivation(imei, DeviceActivationStatus.ACTIVE.name(), reply);
            log.info("Device IMEI {} ACTIVE — lat={} lon={} gsm={}", imei, reply.latitude(), reply.longitude(), reply.gsmSignalBars());
        } else {
            device.setActivationStatus(DeviceActivationStatus.MISCONFIGURED.name());
            deviceRepository.save(device);

            broadcastActivation(imei, DeviceActivationStatus.MISCONFIGURED.name(), reply);
            log.warn("Device IMEI {} MISCONFIGURED — gpsOk={} signalOk={}", imei, gpsOk, signalOk);
        }

        // Clear Redis pending key
        if (device.getSimNumber() != null) {
            redis.delete(REDIS_PREFIX + phoneNumberNormalizer.normalise(device.getSimNumber()));
        }
    }

    private void broadcastActivation(String imei, String status, DeviceStatusReply reply) {
        try {
            ws.convertAndSend("/topic/admin/device-activation",
                    new DeviceActivationEvent(imei, status, reply));
        } catch (Exception e) {
            log.warn("WebSocket broadcast failed for IMEI {}: {}", imei, e.getMessage());
        }
    }
}
