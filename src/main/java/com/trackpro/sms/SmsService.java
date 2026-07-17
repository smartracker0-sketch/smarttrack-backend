package com.trackpro.sms;

import com.trackpro.config.SmsProperties;
import com.trackpro.sms.dto.OutboundSmsResult;
import com.trackpro.sms.dto.SmsDeliveryStatus;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    private final AtomicReference<SmsProvider> activeProvider;
    private final List<SmsProvider> providers;
    private final SmsProperties config;
    private final SmsAuditService auditService;
    private final SmsPhoneNumberNormalizer phoneNumberNormalizer;

    public SmsService(List<SmsProvider> providers,
                      SmsProperties config,
                      SmsAuditService auditService,
                      SmsPhoneNumberNormalizer phoneNumberNormalizer) {
        this.providers = providers;
        this.config = config;
        this.auditService = auditService;
        this.phoneNumberNormalizer = phoneNumberNormalizer;
        this.activeProvider = new AtomicReference<>(selectProvider(config.getProvider()));
        log.info("SMS provider active: {}", activeProvider.get().getProviderName());
    }

    public OutboundSmsResult send(String to, String message) {
        return send(to, message, null);
    }

    public OutboundSmsResult send(String to, String message, String relatedImei) {
        String normalised = normalisePhoneNumber(to);
        int maxAttempts = config.getRetry().getMaxAttempts();
        int attempts = 0;
        OutboundSmsResult lastResult = null;

        while (attempts < maxAttempts) {
            try {
                OutboundSmsResult result = activeProvider.get().send(normalised, message);
                lastResult = result;
                if (result.status() != SmsDeliveryStatus.FAILED) {
                    auditService.recordOutbound(result, message, relatedImei);
                    return result;
                }
                attempts++;
                log.warn("SMS send attempt {} returned FAILED for {}", attempts, normalised);
            } catch (Exception e) {
                attempts++;
                log.warn("SMS send attempt {} threw exception for {}: {}", attempts, normalised, e.getMessage());
            }
            if (attempts < maxAttempts) {
                try {
                    Thread.sleep(config.getRetry().getBackoffSeconds() * 1000L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        log.error("SMS failed after {} attempts to {}", attempts, normalised);
        OutboundSmsResult failed = lastResult != null
                ? lastResult
                : OutboundSmsResult.failed(normalised, activeProvider.get().getProviderName());
        auditService.recordOutbound(failed, message, relatedImei);
        return failed;
    }

    public List<OutboundSmsResult> sendBulk(List<String> recipients, String message) {
        List<String> normalised = recipients.stream().map(this::normalisePhoneNumber).toList();
        List<OutboundSmsResult> results = activeProvider.get().sendBulk(normalised, message);
        results.forEach(result -> auditService.recordOutbound(result, message, null));
        return results;
    }

    public OutboundSmsResult checkStatus(String messageId) {
        return activeProvider.get().checkStatus(messageId);
    }

    public OutboundSmsResult checkStatus(String providerName, String messageId) {
        return selectProvider(providerName).checkStatus(messageId);
    }

    public String getActiveProviderName() {
        return activeProvider.get().getProviderName();
    }

    public void switchProvider(String providerName) {
        SmsProvider next = selectProvider(providerName);
        activeProvider.set(next);
        log.info("SMS provider switched to: {}", providerName);
    }

    private SmsProvider selectProvider(String name) {
        return providers.stream()
                .filter(p -> p.getProviderName().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No SMS provider found for: '" + name + "'. Valid values: "
                        + providers.stream().map(SmsProvider::getProviderName).toList()));
    }

    public String normalisePhoneNumber(String number) {
        return phoneNumberNormalizer.normalise(number);
    }
}
