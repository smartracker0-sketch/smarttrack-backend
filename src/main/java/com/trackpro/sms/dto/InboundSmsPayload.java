package com.trackpro.sms.dto;

import java.time.Instant;

public record InboundSmsPayload(
        String from,
        String to,
        String text,
        String provider,
        Instant receivedAt
) {
    public String getFrom() { return from; }
    public String getText() { return text; }
}
