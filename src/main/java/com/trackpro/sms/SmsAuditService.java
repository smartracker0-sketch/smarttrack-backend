package com.trackpro.sms;

import com.trackpro.model.SmsAuditLog;
import com.trackpro.repository.SmsAuditLogRepository;
import com.trackpro.sms.dto.InboundSmsPayload;
import com.trackpro.sms.dto.OutboundSmsResult;
import com.trackpro.sms.dto.SmsDeliveryStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SmsAuditService {

    private final SmsAuditLogRepository repository;

    public SmsAuditService(SmsAuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void recordOutbound(OutboundSmsResult result, String messageBody, String relatedImei) {
        SmsAuditLog log = new SmsAuditLog();
        log.setDirection("OUTBOUND");
        log.setProvider(result.providerName());
        log.setToNumber(result.to());
        log.setMessageBody(messageBody);
        log.setMessageId(result.messageId());
        log.setStatus(result.status().name());
        log.setRawResponse(result.rawResponse());
        log.setSentAt(result.sentAt() != null ? result.sentAt() : Instant.now());
        log.setDeliveredAt(result.status() == SmsDeliveryStatus.DELIVERED ? Instant.now() : null);
        log.setRelatedImei(relatedImei);
        repository.save(log);
    }

    @Transactional
    public void recordInbound(InboundSmsPayload payload, String relatedImei) {
        SmsAuditLog log = new SmsAuditLog();
        log.setDirection("INBOUND");
        log.setProvider(payload.provider());
        log.setFromNumber(payload.from());
        log.setToNumber(payload.to());
        log.setMessageBody(payload.text());
        log.setStatus("RECEIVED");
        log.setSentAt(payload.receivedAt() != null ? payload.receivedAt() : Instant.now());
        log.setRelatedImei(relatedImei);
        repository.save(log);
    }

    @Transactional(readOnly = true)
    public List<SmsAuditLog> findDeliveryChecksDue(Instant since) {
        return repository.findDeliveryChecksDue(since);
    }

    @Transactional
    public void updateDeliveryStatus(SmsAuditLog log, OutboundSmsResult result) {
        log.setStatus(result.status().name());
        log.setRawResponse(result.rawResponse());
        if (result.status() == SmsDeliveryStatus.DELIVERED) {
            log.setDeliveredAt(Instant.now());
        }
        repository.save(log);
    }
}
