package com.trackpro.sms;

import com.trackpro.config.SmsProperties;
import com.trackpro.sms.dto.OutboundSmsResult;
import com.trackpro.sms.dto.SmsDeliveryStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class SmsServiceTest {

    private SmsProvider termiiProvider;
    private SmsProvider atProvider;
    private SmsProperties props;

    @BeforeEach
    void setUp() {
        termiiProvider = mock(SmsProvider.class);
        when(termiiProvider.getProviderName()).thenReturn("termii");

        atProvider = mock(SmsProvider.class);
        when(atProvider.getProviderName()).thenReturn("africastalking");

        props = new SmsProperties();
        props.getRetry().setMaxAttempts(3);
        props.getRetry().setBackoffSeconds(0);
    }

    @Test
    void selectsTermiiProviderByConfig() {
        props.setProvider("termii");
        SmsService service = new SmsService(List.of(termiiProvider, atProvider), props);
        assertThat(service.getActiveProviderName()).isEqualTo("termii");
    }

    @Test
    void selectsAfricasTalkingProviderByConfig() {
        props.setProvider("africastalking");
        SmsService service = new SmsService(List.of(termiiProvider, atProvider), props);
        assertThat(service.getActiveProviderName()).isEqualTo("africastalking");
    }

    @Test
    void throwsOnUnknownProvider() {
        props.setProvider("unknown");
        assertThatThrownBy(() -> new SmsService(List.of(termiiProvider, atProvider), props))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unknown");
    }

    @Test
    void switchProviderAtRuntime() {
        props.setProvider("termii");
        SmsService service = new SmsService(List.of(termiiProvider, atProvider), props);
        assertThat(service.getActiveProviderName()).isEqualTo("termii");

        service.switchProvider("africastalking");
        assertThat(service.getActiveProviderName()).isEqualTo("africastalking");
    }

    @Test
    void retriesOnFailureThenReturnsFailed() {
        props.setProvider("termii");
        when(termiiProvider.send(anyString(), anyString()))
                .thenReturn(OutboundSmsResult.failed("+2348012345678", "termii"));

        SmsService service = new SmsService(List.of(termiiProvider, atProvider), props);
        OutboundSmsResult result = service.send("+2348012345678", "TEST");

        verify(termiiProvider, times(3)).send(anyString(), anyString());
        assertThat(result.status()).isEqualTo(SmsDeliveryStatus.FAILED);
    }

    @Test
    void stopsRetryingOnSuccess() {
        props.setProvider("termii");
        OutboundSmsResult success = new OutboundSmsResult("msg-1", "+2348012345678",
                SmsDeliveryStatus.SENT, "termii", "{}", Instant.now());
        when(termiiProvider.send(anyString(), anyString())).thenReturn(success);

        SmsService service = new SmsService(List.of(termiiProvider, atProvider), props);
        OutboundSmsResult result = service.send("+2348012345678", "TEST");

        verify(termiiProvider, times(1)).send(anyString(), anyString());
        assertThat(result.status()).isEqualTo(SmsDeliveryStatus.SENT);
    }

    @Test
    void normalisesNigerianLocalFormat() {
        props.setProvider("termii");
        SmsService service = new SmsService(List.of(termiiProvider, atProvider), props);
        assertThat(service.normalisePhoneNumber("08012345678")).isEqualTo("+2348012345678");
    }

    @Test
    void normalisesNigerianWithoutPlus() {
        props.setProvider("termii");
        SmsService service = new SmsService(List.of(termiiProvider, atProvider), props);
        assertThat(service.normalisePhoneNumber("2348012345678")).isEqualTo("+2348012345678");
    }

    @Test
    void leavesInternationalFormatUnchanged() {
        props.setProvider("termii");
        SmsService service = new SmsService(List.of(termiiProvider, atProvider), props);
        assertThat(service.normalisePhoneNumber("+2348012345678")).isEqualTo("+2348012345678");
    }

    @Test
    void normalisesWithSpacesAndDashes() {
        props.setProvider("termii");
        SmsService service = new SmsService(List.of(termiiProvider, atProvider), props);
        assertThat(service.normalisePhoneNumber("080-1234-5678")).isEqualTo("+2348012345678");
    }
}
