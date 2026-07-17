package com.trackpro.sms;

import com.trackpro.config.SmsProperties;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SmsWebhookVerifierTest {

    @Test
    void allowsWebhookWhenNoSecretIsConfigured() {
        SmsProperties props = new SmsProperties();
        SmsWebhookVerifier verifier = new SmsWebhookVerifier(props);

        assertThat(verifier.isValid(Map.of(), Map.of())).isTrue();
    }

    @Test
    void acceptsConfiguredSecretFromHeader() {
        SmsProperties props = new SmsProperties();
        props.getWebhook().setSharedSecret("secret-123");
        SmsWebhookVerifier verifier = new SmsWebhookVerifier(props);

        assertThat(verifier.isValid(Map.of("X-TrackPro-Sms-Secret", "secret-123"), Map.of())).isTrue();
    }

    @Test
    void acceptsConfiguredSecretFromParam() {
        SmsProperties props = new SmsProperties();
        props.getWebhook().setSharedSecret("secret-123");
        SmsWebhookVerifier verifier = new SmsWebhookVerifier(props);

        assertThat(verifier.isValid(Map.of(), Map.of("secret", "secret-123"))).isTrue();
    }

    @Test
    void rejectsMissingConfiguredSecret() {
        SmsProperties props = new SmsProperties();
        props.getWebhook().setSharedSecret("secret-123");
        SmsWebhookVerifier verifier = new SmsWebhookVerifier(props);

        assertThat(verifier.isValid(Map.of(), Map.of())).isFalse();
    }
}
