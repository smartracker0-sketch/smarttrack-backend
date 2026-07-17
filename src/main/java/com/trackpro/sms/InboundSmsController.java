package com.trackpro.sms;

import com.trackpro.exception.NotFoundException;
import com.trackpro.repository.DeviceRepository;
import com.trackpro.sms.dto.InboundSmsPayload;
import com.trackpro.sms.webhook.AfricasTalkingWebhookParser;
import com.trackpro.sms.webhook.TermiiWebhookParser;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sms")
public class InboundSmsController {

    private static final Logger log = LoggerFactory.getLogger(InboundSmsController.class);
    private static final String REDIS_PREFIX = "device:activation:pending:";

    private final StringRedisTemplate redis;
    private final DeviceActivationService activationService;
    private final DeviceRepository deviceRepository;
    private final TermiiWebhookParser termiiParser;
    private final AfricasTalkingWebhookParser atParser;
    private final SmsWebhookVerifier webhookVerifier;
    private final SmsPhoneNumberNormalizer phoneNumberNormalizer;
    private final SmsAuditService auditService;

    public InboundSmsController(StringRedisTemplate redis,
                                DeviceActivationService activationService,
                                DeviceRepository deviceRepository,
                                TermiiWebhookParser termiiParser,
                                AfricasTalkingWebhookParser atParser,
                                SmsWebhookVerifier webhookVerifier,
                                SmsPhoneNumberNormalizer phoneNumberNormalizer,
                                SmsAuditService auditService) {
        this.redis = redis;
        this.activationService = activationService;
        this.deviceRepository = deviceRepository;
        this.termiiParser = termiiParser;
        this.atParser = atParser;
        this.webhookVerifier = webhookVerifier;
        this.phoneNumberNormalizer = phoneNumberNormalizer;
        this.auditService = auditService;
    }

    /**
     * Termii inbound webhook — configure in Termii dashboard as:
     * https://api.smarttracker.cloud/api/v1/sms/inbound/termii
     */
    @PostMapping("/inbound/termii")
    public ResponseEntity<Void> termiiInbound(@RequestBody Map<String, Object> raw,
                                             @RequestHeader Map<String, String> headers,
                                             @RequestParam Map<String, String> params) {
        if (!webhookVerifier.isValid(headers, params)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        InboundSmsPayload payload = termiiParser.parse(raw);
        return routeInbound(payload);
    }

    /**
     * Africa's Talking inbound webhook — configure in AT dashboard as:
     * https://api.smarttracker.cloud/api/v1/sms/inbound/africastalking
     * AT sends form params, not JSON body.
     */
    @PostMapping("/inbound/africastalking")
    public ResponseEntity<Void> atInbound(@RequestParam Map<String, String> raw,
                                          @RequestHeader Map<String, String> headers) {
        if (!webhookVerifier.isValid(headers, raw)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        InboundSmsPayload payload = atParser.parse(raw);
        return routeInbound(payload);
    }

    /**
     * Legacy Termii webhook path — kept for backward compat.
     */
    @PostMapping("/inbound")
    public ResponseEntity<Void> legacyInbound(@RequestBody Map<String, Object> raw,
                                             @RequestHeader Map<String, String> headers,
                                             @RequestParam Map<String, String> params) {
        if (!webhookVerifier.isValid(headers, params)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        InboundSmsPayload payload = termiiParser.parse(raw);
        return routeInbound(payload);
    }

    private ResponseEntity<Void> routeInbound(InboundSmsPayload payload) {
        String from = payload.from();
        String text = payload.text();
        log.info("Inbound SMS provider={} from={} text={}", payload.provider(), from, text);

        if (from == null || text == null) {
            return ResponseEntity.badRequest().build();
        }

        String normalisedFrom = phoneNumberNormalizer.normalise(from);
        String imei = redis.opsForValue().get(REDIS_PREFIX + normalisedFrom);
        if (imei == null && !normalisedFrom.equals(from)) {
            imei = redis.opsForValue().get(REDIS_PREFIX + from);
        }

        auditService.recordInbound(payload, imei);

        if (imei != null) {
            activationService.handleActivationReply(imei, text);
        } else {
            log.info("Inbound SMS from {} is not an activation reply (no pending IMEI in cache)", from);
        }

        return ResponseEntity.ok().build();
    }

    /**
     * Admin manually triggers an activation check for a device by IMEI.
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
