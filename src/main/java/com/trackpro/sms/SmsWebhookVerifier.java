package com.trackpro.sms;

import com.trackpro.config.SmsProperties;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SmsWebhookVerifier {

    private final SmsProperties props;

    public SmsWebhookVerifier(SmsProperties props) {
        this.props = props;
    }

    public boolean isValid(Map<String, String> headers, Map<String, String> params) {
        String expected = props.getWebhook().getSharedSecret();
        if (expected == null || expected.isBlank()) {
            return true;
        }
        String provided = firstPresent(headers, "x-trackpro-sms-secret", "x-sms-webhook-secret");
        if (provided == null || provided.isBlank()) {
            provided = firstPresent(params, "secret", "webhookSecret", "sms_secret");
        }
        return expected.equals(provided);
    }

    private String firstPresent(Map<String, String> values, String... keys) {
        if (values == null || values.isEmpty()) return null;
        for (String key : keys) {
            String value = values.get(key);
            if (value != null) return value;
            for (Map.Entry<String, String> entry : values.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(key)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }
}
