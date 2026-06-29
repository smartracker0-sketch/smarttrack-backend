package com.trackpro.sms.webhook;

import com.trackpro.sms.dto.InboundSmsPayload;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class TermiiWebhookParser {

    @SuppressWarnings("unchecked")
    public InboundSmsPayload parse(Map<String, Object> raw) {
        Map<String, Object> data = raw.containsKey("data")
                ? (Map<String, Object>) raw.get("data")
                : raw;

        String from = (String) data.get("sender");
        String to   = (String) data.get("receiver");
        String text = (String) data.get("text");

        return new InboundSmsPayload(from, to, text, "termii", Instant.now());
    }
}
