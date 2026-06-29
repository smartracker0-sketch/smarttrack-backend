package com.trackpro.sms.dto;

import java.time.Instant;

public record OutboundSmsResult(
        String messageId,
        String to,
        SmsDeliveryStatus status,
        String providerName,
        String rawResponse,
        Instant sentAt
) {
    public static OutboundSmsResult failed(String to, String providerName) {
        return new OutboundSmsResult(null, to, SmsDeliveryStatus.FAILED, providerName, null, Instant.now());
    }
}
