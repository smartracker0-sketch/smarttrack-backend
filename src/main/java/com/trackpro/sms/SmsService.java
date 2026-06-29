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

    public SmsService(List<SmsProvider> providers, SmsProperties config) {
        this.providers = providers;
        this.config = config;
        this.activeProvider = new AtomicReference<>(selectProvider(config.getProvider()));
        log.info("SMS provider active: {}", activeProvider.get().getProviderName());
    }

    public OutboundSmsResult send(String to, String message) {
        String normalised = normalisePhoneNumber(to);
        int maxAttempts = config.getRetry().getMaxAttempts();
        int attempts = 0;

        while (attempts < maxAttempts) {
            try {
                OutboundSmsResult result = activeProvider.get().send(normalised, message);
                if (result.status() != SmsDeliveryStatus.FAILED) {
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
        return OutboundSmsResult.failed(normalised, activeProvider.get().getProviderName());
    }

    public List<OutboundSmsResult> sendBulk(List<String> recipients, String message) {
        List<String> normalised = recipients.stream().map(this::normalisePhoneNumber).toList();
        return activeProvider.get().sendBulk(normalised, message);
    }

    public OutboundSmsResult checkStatus(String messageId) {
        return activeProvider.get().checkStatus(messageId);
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

    String normalisePhoneNumber(String number) {
        if (number == null) return "";
        String cleaned = number.replaceAll("[^0-9+]", "");
        if (cleaned.startsWith("0")) {
            return "+234" + cleaned.substring(1);
        } else if (cleaned.startsWith("234") && !cleaned.startsWith("+")) {
            return "+" + cleaned;
        }
        return cleaned;
    }
}
