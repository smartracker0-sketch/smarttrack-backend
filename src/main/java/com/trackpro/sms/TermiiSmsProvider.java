package com.trackpro.sms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackpro.config.SmsProperties;
import com.trackpro.sms.dto.OutboundSmsRequest;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "trackpro.sms.provider", havingValue = "termii", matchIfMissing = true)
public class TermiiSmsProvider implements SmsProvider {

    private static final Logger log = LoggerFactory.getLogger(TermiiSmsProvider.class);

    private final SmsProperties props;
    private final ObjectMapper mapper;
    private final HttpClient httpClient;

    public TermiiSmsProvider(SmsProperties props, ObjectMapper mapper) {
        this.props = props;
        this.mapper = mapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public void send(String to, String message) {
        String apiKey = props.getTermii().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("SMS skipped — TERMII_API_KEY not configured. Would send '{}' to {}", message, to);
            return;
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
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> resp = httpClient.send(httpReq, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                log.info("SMS sent to {} — status {}", to, resp.statusCode());
            } else {
                log.warn("SMS to {} failed — HTTP {} body={}", to, resp.statusCode(), resp.body());
            }
        } catch (Exception e) {
            log.error("SMS send error to {}: {}", to, e.getMessage(), e);
        }
    }
}
