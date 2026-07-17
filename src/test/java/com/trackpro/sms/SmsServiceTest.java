package com.trackpro.sms;

import com.trackpro.config.SmsProperties;
import com.trackpro.repository.SmsAuditLogRepository;
import com.trackpro.sms.dto.OutboundSmsResult;
import com.trackpro.sms.dto.SmsDeliveryStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SmsServiceTest {

    private FakeSmsProvider termiiProvider;
    private FakeSmsProvider atProvider;
    private SmsProperties props;
    private SmsService service;

    @BeforeEach
    void setUp() {
        termiiProvider = new FakeSmsProvider("termii");
        atProvider = new FakeSmsProvider("africastalking");
        props = new SmsProperties();
        props.getRetry().setMaxAttempts(3);
        props.getRetry().setBackoffSeconds(0);
    }

    @Test
    void selectsTermiiProviderByConfig() {
        props.setProvider("termii");
        service = service();
        assertThat(service.getActiveProviderName()).isEqualTo("termii");
    }

    @Test
    void selectsAfricasTalkingProviderByConfig() {
        props.setProvider("africastalking");
        service = service();
        assertThat(service.getActiveProviderName()).isEqualTo("africastalking");
    }

    @Test
    void throwsOnUnknownProvider() {
        props.setProvider("unknown");
        assertThatThrownBy(this::service)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unknown");
    }

    @Test
    void switchProviderAtRuntime() {
        props.setProvider("termii");
        service = service();
        service.switchProvider("africastalking");
        assertThat(service.getActiveProviderName()).isEqualTo("africastalking");
    }

    @Test
    void retriesOnFailureThenReturnsFailed() {
        props.setProvider("termii");
        termiiProvider.nextResults = List.of(
                OutboundSmsResult.failed("+2348012345678", "termii"),
                OutboundSmsResult.failed("+2348012345678", "termii"),
                OutboundSmsResult.failed("+2348012345678", "termii")
        );

        service = service();
        OutboundSmsResult result = service.send("+2348012345678", "TEST");

        assertThat(termiiProvider.sendCount).isEqualTo(3);
        assertThat(result.status()).isEqualTo(SmsDeliveryStatus.FAILED);
    }

    @Test
    void stopsRetryingOnSuccess() {
        props.setProvider("termii");
        termiiProvider.nextResults = List.of(new OutboundSmsResult("msg-1", "+2348012345678",
                SmsDeliveryStatus.SENT, "termii", "{}", Instant.now()));

        service = service();
        OutboundSmsResult result = service.send("+2348012345678", "TEST");

        assertThat(termiiProvider.sendCount).isEqualTo(1);
        assertThat(result.status()).isEqualTo(SmsDeliveryStatus.SENT);
    }

    @Test
    void normalisesNigerianLocalFormat() {
        props.setProvider("termii");
        service = service();
        assertThat(service.normalisePhoneNumber("08012345678")).isEqualTo("+2348012345678");
    }

    @Test
    void normalisesNigerianWithoutPlus() {
        props.setProvider("termii");
        service = service();
        assertThat(service.normalisePhoneNumber("2348012345678")).isEqualTo("+2348012345678");
    }

    @Test
    void leavesInternationalFormatUnchanged() {
        props.setProvider("termii");
        service = service();
        assertThat(service.normalisePhoneNumber("+2348012345678")).isEqualTo("+2348012345678");
    }

    @Test
    void normalisesWithSpacesAndDashes() {
        props.setProvider("termii");
        service = service();
        assertThat(service.normalisePhoneNumber("080-1234-5678")).isEqualTo("+2348012345678");
    }

    private SmsService service() {
        return new SmsService(
                List.of(termiiProvider, atProvider),
                props,
                new NoopSmsAuditService(),
                new SmsPhoneNumberNormalizer()
        );
    }

    private static class FakeSmsProvider implements SmsProvider {
        private final String name;
        private List<OutboundSmsResult> nextResults = new ArrayList<>();
        private int sendCount = 0;

        private FakeSmsProvider(String name) {
            this.name = name;
        }

        @Override
        public String getProviderName() {
            return name;
        }

        @Override
        public OutboundSmsResult send(String to, String message) {
            sendCount++;
            if (!nextResults.isEmpty()) {
                int index = Math.min(sendCount - 1, nextResults.size() - 1);
                return nextResults.get(index);
            }
            return new OutboundSmsResult("msg-" + sendCount, to, SmsDeliveryStatus.SENT, name, "{}", Instant.now());
        }

        @Override
        public OutboundSmsResult checkStatus(String messageId) {
            return new OutboundSmsResult(messageId, null, SmsDeliveryStatus.DELIVERED, name, "{}", Instant.now());
        }
    }

    private static class NoopSmsAuditService extends SmsAuditService {
        private NoopSmsAuditService() {
            super((SmsAuditLogRepository) null);
        }

        @Override
        public void recordOutbound(OutboundSmsResult result, String messageBody, String relatedImei) {
        }
    }
}
