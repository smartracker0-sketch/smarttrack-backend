package com.trackpro.sms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackpro.config.SmsProperties;
import com.trackpro.sms.dto.OutboundSmsRequest;
import com.trackpro.sms.dto.OutboundSmsResult;
import com.trackpro.sms.dto.SmsDeliveryStatus;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TermiiSmsProvider implements SmsProvider {

    private static final Logger log = LoggerFactory.getLogger(TermiiSmsProvider.class);

    private final SmsProperties props;
    private final ObjectMapper mapper;
    private final HttpClient httpClient;

    public TermiiSmsProvider(SmsProperties props, ObjectMapper mapper) {
        this.props = props;
        this.mapper = mapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(props.getTermii().getTimeoutSeconds()))
                .build();
    }

    @Override
    public String getProviderName() {
        return "termii";
    }

    @Override
    public OutboundSmsResult send(String to, String message) {
        String apiKey = props.getTermii().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("SMS skipped — TERMII_API_KEY not configured. Would send '{}' to {}", message, to);
            return OutboundSmsResult.failed(to, getProviderName());
        }

        try {
            OutboundSmsRequest req = new OutboundSmsRequest(
                    apiKey,
                    props.getTermii().getSenderId(),
                    to,
                    message
            );
            String body = mapper.writeValueAsString(req);
            HttpRequest httpReq = HttpRequest.newBuilder()
                    .uri(URI.create(props.getTermii().getBaseUrl() + "/api/sms/send"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(props.getTermii().getTimeoutSeconds() + 5L))
                    .build();

            HttpResponse<String> resp = httpClient.send(httpReq, HttpResponse.BodyHandlers.ofString());
            String rawBody = resp.body();

            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                log.info("Termii SMS sent to {} — status {}", to, resp.statusCode());
                String messageId = extractMessageId(rawBody);
                return new OutboundSmsResult(messageId, to, SmsDeliveryStatus.SENT, getProviderName(), rawBody, Instant.now());
            } else {
                log.warn("Termii SMS to {} failed — HTTP {} body={}", to, resp.statusCode(), rawBody);
                return new OutboundSmsResult(null, to, SmsDeliveryStatus.FAILED, getProviderName(), rawBody, Instant.now());
            }
        } catch (Exception e) {
            log.error("Termii SMS send error to {}: {}", to, e.getMessage(), e);
            return OutboundSmsResult.failed(to, getProviderName());
        }
    }

    @Override
    public OutboundSmsResult checkStatus(String messageId) {
        String apiKey = props.getTermii().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return OutboundSmsResult.failed(null, getProviderName());
        }
        try {
            String url = props.getTermii().getBaseUrl()
                    + "/api/sms/inbox?api_key=" + apiKey + "&message_id=" + messageId;
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(props.getTermii().getTimeoutSeconds()))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            SmsDeliveryStatus status = resp.statusCode() < 300 ? SmsDeliveryStatus.DELIVERED : SmsDeliveryStatus.PENDING;
            return new OutboundSmsResult(messageId, null, status, getProviderName(), resp.body(), Instant.now());
        } catch (Exception e) {
            log.error("Termii checkStatus error for {}: {}", messageId, e.getMessage());
            return OutboundSmsResult.failed(null, getProviderName());
        }
    }

    @SuppressWarnings("unchecked")
    private String extractMessageId(String rawBody) {
        try {
            Map<String, Object> parsed = mapper.readValue(rawBody, Map.class);
            Object id = parsed.get("message_id");
            return id != null ? id.toString() : null;
        } catch (Exception ignored) {
            return null;
        }
    }
}
