package com.trackpro.sms;

import com.trackpro.sms.dto.InboundSmsPayload;
import com.trackpro.sms.webhook.AfricasTalkingWebhookParser;
import com.trackpro.sms.webhook.TermiiWebhookParser;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookParserTest {

    private final TermiiWebhookParser termiiParser = new TermiiWebhookParser();
    private final AfricasTalkingWebhookParser atParser = new AfricasTalkingWebhookParser();

    @Test
    void termiiParsesNestedDataFormat() {
        Map<String, Object> raw = Map.of("data", Map.of(
                "sender", "+2348012345678",
                "receiver", "STTracker",
                "text", "Latitude:6.45 Longitude:3.39 Speed:0 GSM:5"
        ));

        InboundSmsPayload payload = termiiParser.parse(raw);

        assertThat(payload.from()).isEqualTo("+2348012345678");
        assertThat(payload.to()).isEqualTo("STTracker");
        assertThat(payload.text()).isEqualTo("Latitude:6.45 Longitude:3.39 Speed:0 GSM:5");
        assertThat(payload.provider()).isEqualTo("termii");
        assertThat(payload.receivedAt()).isNotNull();
    }

    @Test
    void termiiParsesFlatFormat() {
        Map<String, Object> raw = Map.of(
                "sender", "+2348012345678",
                "receiver", "STTracker",
                "text", "STATUS OK"
        );

        InboundSmsPayload payload = termiiParser.parse(raw);

        assertThat(payload.from()).isEqualTo("+2348012345678");
        assertThat(payload.text()).isEqualTo("STATUS OK");
        assertThat(payload.provider()).isEqualTo("termii");
    }

    @Test
    void africasTalkingParsesFormParams() {
        Map<String, String> raw = Map.of(
                "from", "+2348012345678",
                "to", "STTracker",
                "text", "Latitude:6.45 Longitude:3.39 Speed:0 GSM:5",
                "date", "2026-06-29T10:30:00Z",
                "id", "ATXid_abc123"
        );

        InboundSmsPayload payload = atParser.parse(raw);

        assertThat(payload.from()).isEqualTo("+2348012345678");
        assertThat(payload.to()).isEqualTo("STTracker");
        assertThat(payload.text()).isEqualTo("Latitude:6.45 Longitude:3.39 Speed:0 GSM:5");
        assertThat(payload.provider()).isEqualTo("africastalking");
        assertThat(payload.receivedAt().toString()).startsWith("2026-06-29");
    }

    @Test
    void africasTalkingHandlesMissingDate() {
        Map<String, String> raw = Map.of(
                "from", "+2348012345678",
                "to", "STTracker",
                "text", "OK"
        );

        InboundSmsPayload payload = atParser.parse(raw);

        assertThat(payload.from()).isEqualTo("+2348012345678");
        assertThat(payload.receivedAt()).isNotNull();
    }
}
