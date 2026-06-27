package com.trackpro.sms;

import com.trackpro.exception.NotFoundException;
import com.trackpro.repository.DeviceRepository;
import com.trackpro.sms.dto.InboundSmsPayload;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sms")
public class InboundSmsController {

    private static final Logger log = LoggerFactory.getLogger(InboundSmsController.class);
    private static final String REDIS_PREFIX = "device:activation:pending:";

    private final StringRedisTemplate redis;
    private final DeviceActivationService activationService;
    private final DeviceRepository deviceRepository;

    public InboundSmsController(StringRedisTemplate redis,
                                DeviceActivationService activationService,
                                DeviceRepository deviceRepository) {
        this.redis = redis;
        this.activationService = activationService;
        this.deviceRepository = deviceRepository;
    }

    /**
     * Termii webhook — called when a device SIM sends an inbound SMS reply.
     * This endpoint is intentionally public (no JWT) because Termii posts to it.
     */
    @PostMapping("/inbound")
    public ResponseEntity<Void> receiveInbound(@RequestBody InboundSmsPayload payload) {
        String from = payload.getFrom();
        String text = payload.getText();
        log.info("Inbound SMS from={} text={}", from, text);

        if (from == null || text == null) {
            return ResponseEntity.badRequest().build();
        }

        String imei = redis.opsForValue().get(REDIS_PREFIX + from);
        if (imei != null) {
            activationService.handleActivationReply(imei, text);
        } else {
            log.info("Inbound SMS from {} is not an activation reply (no pending IMEI in cache)", from);
        }

        return ResponseEntity.ok().build();
    }

    /**
     * Admin manually triggers an activation check for a device by IMEI.
     * POST /api/v1/sms/devices/{imei}/check
     */
    @PostMapping("/devices/{imei}/check")
    @PreAuthorize("hasAnyRole('ROLE_SUPER_ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<Map<String, String>> triggerCheck(@PathVariable String imei) {
        var device = deviceRepository.findByImei(imei)
                .orElseThrow(() -> new NotFoundException("Device not found: " + imei));

        device.setActivationStatus(DeviceActivationStatus.PENDING.name());
        device.setActivationAttempts(0);
        deviceRepository.save(device);

        activationService.initiateCheck(device);

        log.info("Manual activation check triggered for IMEI {}", imei);
        return ResponseEntity.accepted()
                .body(Map.of("message", "Activation check initiated", "imei", imei));
    }
}
