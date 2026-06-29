package com.trackpro.sms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackpro.config.SmsProperties;
import com.trackpro.sms.dto.OutboundSmsResult;
import com.trackpro.sms.dto.SmsDeliveryStatus;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AfricasTalkingSmsProvider implements SmsProvider {

    private static final Logger log = LoggerFactory.getLogger(AfricasTalkingSmsProvider.class);

    private final SmsProperties props;
    private final ObjectMapper mapper;
    private final HttpClient httpClient;

    public AfricasTalkingSmsProvider(SmsProperties props, ObjectMapper mapper) {
        this.props = props;
        this.mapper = mapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(props.getAfricastalking().getTimeoutSeconds()))
                .build();
    }

    @Override
    public String getProviderName() {
        return "africastalking";
    }

    @Override
    public OutboundSmsResult send(String to, String message) {
        return sendToNumbers(to, message);
    }

    @Override
    public List<OutboundSmsResult> sendBulk(List<String> recipients, String message) {
        String joined = String.join(",", recipients);
        OutboundSmsResult bulk = sendToNumbers(joined, message);
        return recipients.stream()
                .map(r -> new OutboundSmsResult(bulk.messageId(), r, bulk.status(), getProviderName(), bulk.rawResponse(), bulk.sentAt()))
                .toList();
    }

    @Override
    public OutboundSmsResult checkStatus(String messageId) {
        SmsProperties.AfricasTalking at = props.getAfricastalking();
        if (at.getApiKey() == null || at.getApiKey().isBlank()) {
            return OutboundSmsResult.failed(null, getProviderName());
        }
        try {
            String baseUrl = resolveBaseUrl();
            String url = baseUrl + "/version1/messaging?username=" + encode(at.getUsername())
                    + "&messageId=" + encode(messageId);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("apiKey", at.getApiKey())
                    .header("Accept", "application/json")
                    .GET()
                    .timeout(Duration.ofSeconds(at.getTimeoutSeconds()))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            SmsDeliveryStatus status = resp.statusCode() < 300 ? SmsDeliveryStatus.DELIVERED : SmsDeliveryStatus.PENDING;
            return new OutboundSmsResult(messageId, null, status, getProviderName(), resp.body(), Instant.now());
        } catch (Exception e) {
            log.error("AT checkStatus error for {}: {}", messageId, e.getMessage());
            return OutboundSmsResult.failed(null, getProviderName());
        }
    }

    @SuppressWarnings("unchecked")
    private OutboundSmsResult sendToNumbers(String to, String message) {
        SmsProperties.AfricasTalking at = props.getAfricastalking();
        if (at.getApiKey() == null || at.getApiKey().isBlank()) {
            log.warn("SMS skipped — AT_API_KEY not configured. Would send '{}' to {}", message, to);
            return OutboundSmsResult.failed(to, getProviderName());
        }

        try {
            String baseUrl = resolveBaseUrl();
            StringBuilder form = new StringBuilder();
            form.append("username=").append(encode(at.getUsername()));
            form.append("&to=").append(encode(to));
            form.append("&message=").append(encode(message));
            if (at.getSenderId() != null && !at.getSenderId().isBlank()) {
                form.append("&from=").append(encode(at.getSenderId()));
            }

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/version1/messaging"))
                    .header("apiKey", at.getApiKey())
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(form.toString()))
                    .timeout(Duration.ofSeconds(at.getTimeoutSeconds() + 5L))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            String rawBody = resp.body();

            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                Map<String, Object> parsed = mapper.readValue(rawBody, Map.class);
                Map<String, Object> smsData = (Map<String, Object>) parsed.get("SMSMessageData");
                List<Map<String, Object>> recipients = (List<Map<String, Object>>) smsData.get("Recipients");

                if (recipients != null && !recipients.isEmpty()) {
                    Map<String, Object> first = recipients.get(0);
                    String messageId = (String) first.get("messageId");
                    int statusCode = first.get("statusCode") instanceof Number n ? n.intValue() : -1;
                    SmsDeliveryStatus delivery = statusCode == 101 ? SmsDeliveryStatus.SENT : SmsDeliveryStatus.FAILED;
                    log.info("AT SMS sent to {} — statusCode={}", to, statusCode);
                    return new OutboundSmsResult(messageId, to, delivery, getProviderName(), rawBody, Instant.now());
                }
                return new OutboundSmsResult(null, to, SmsDeliveryStatus.SENT, getProviderName(), rawBody, Instant.now());
            } else {
                log.warn("AT SMS to {} failed — HTTP {} body={}", to, resp.statusCode(), rawBody);
                return new OutboundSmsResult(null, to, SmsDeliveryStatus.FAILED, getProviderName(), rawBody, Instant.now());
            }
        } catch (Exception e) {
            log.error("AT SMS send error to {}: {}", to, e.getMessage(), e);
            return OutboundSmsResult.failed(to, getProviderName());
        }
    }

    private String resolveBaseUrl() {
        SmsProperties.AfricasTalking at = props.getAfricastalking();
        return at.isSandboxMode()
                ? "https://api.sandbox.africastalking.com"
                : at.getBaseUrl();
    }

    private String encode(String value) {
        if (value == null) return "";
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
