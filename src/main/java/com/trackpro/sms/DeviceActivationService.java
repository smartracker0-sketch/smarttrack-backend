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

    private final SmsProvider smsProvider;
    private final DeviceRepository deviceRepository;
    private final StringRedisTemplate redis;
    private final SimpMessagingTemplate ws;
    private final InboundSmsParser parser;
    private final SmsProperties props;

    public DeviceActivationService(SmsProvider smsProvider,
                                   DeviceRepository deviceRepository,
                                   StringRedisTemplate redis,
                                   SimpMessagingTemplate ws,
                                   InboundSmsParser parser,
                                   SmsProperties props) {
        this.smsProvider = smsProvider;
        this.deviceRepository = deviceRepository;
        this.redis = redis;
        this.ws = ws;
        this.parser = parser;
        this.props = props;
    }

    @Async("smsExecutor")
    public void initiateCheck(DeviceEntity device) {
        if (device.getSimNumber() == null || device.getSimNumber().isBlank()) {
            log.warn("Activation skipped for IMEI {} — no SIM number recorded", device.getImei());
            return;
        }

        try {
            // Step 1: Send SERVER configuration command to point device at our TCP server
            String serverHost = props.getServer().getHost();
            int tcpPort = props.getServer().getTcpPort();
            if (!serverHost.isBlank()) {
                String serverCmd = String.format("SERVER,1,%s,%d,0", serverHost, tcpPort);
                smsProvider.send(device.getSimNumber(), serverCmd);
                Thread.sleep(5_000);
                device.setServerConfigured(true);
            }

            // Step 2: Send APN command if configured
            if (device.getSimApn() != null && !device.getSimApn().isBlank()) {
                smsProvider.send(device.getSimNumber(), "APN," + device.getSimApn());
                Thread.sleep(5_000);
                device.setApnConfigured(true);
            }

            // Step 3: Send STATUS query
            smsProvider.send(device.getSimNumber(), props.getActivation().getCommand());

            // Step 4: Update DB — PENDING state
            device.setActivationStatus(DeviceActivationStatus.PENDING.name());
            device.setActivationAttemptedAt(Instant.now());
            deviceRepository.save(device);

            // Step 5: Cache SIM → IMEI mapping in Redis for the inbound webhook to look up
            redis.opsForValue().set(
                    REDIS_PREFIX + device.getSimNumber(),
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
            redis.delete(REDIS_PREFIX + device.getSimNumber());
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
