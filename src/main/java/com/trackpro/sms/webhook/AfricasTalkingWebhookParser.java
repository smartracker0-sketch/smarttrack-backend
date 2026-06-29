package com.trackpro.sms.webhook;

import com.trackpro.sms.dto.InboundSmsPayload;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AfricasTalkingWebhookParser {

    public InboundSmsPayload parse(Map<String, String> raw) {
        String from = raw.get("from");
        String to   = raw.get("to");
        String text = raw.get("text");

        Instant receivedAt;
        try {
            String date = raw.get("date");
            receivedAt = date != null ? Instant.parse(date) : Instant.now();
        } catch (Exception ignored) {
            receivedAt = Instant.now();
        }

        return new InboundSmsPayload(from, to, text, "africastalking", receivedAt);
    }
}
