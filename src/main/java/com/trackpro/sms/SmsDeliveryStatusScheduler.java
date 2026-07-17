package com.trackpro.sms;

import com.trackpro.config.SmsProperties;
import com.trackpro.model.SmsAuditLog;
import com.trackpro.sms.dto.OutboundSmsResult;
import com.trackpro.sms.dto.SmsDeliveryStatus;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SmsDeliveryStatusScheduler {

    private static final Logger log = LoggerFactory.getLogger(SmsDeliveryStatusScheduler.class);

    private final SmsAuditService auditService;
    private final SmsService smsService;
    private final SmsProperties props;

    public SmsDeliveryStatusScheduler(SmsAuditService auditService, SmsService smsService, SmsProperties props) {
        this.auditService = auditService;
        this.smsService = smsService;
        this.props = props;
    }

    @Scheduled(fixedRateString = "${trackpro.sms.delivery-status.poll-rate-ms:300000}")
    public void refreshDeliveryStatuses() {
        if (!props.getDeliveryStatus().isEnabled()) {
            return;
        }

        Instant since = Instant.now().minus(Duration.ofHours(props.getDeliveryStatus().getLookbackHours()));
        for (SmsAuditLog audit : auditService.findDeliveryChecksDue(since)) {
            try {
                OutboundSmsResult result = smsService.checkStatus(audit.getProvider(), audit.getMessageId());
                if (result.status() != SmsDeliveryStatus.PENDING) {
                    auditService.updateDeliveryStatus(audit, result);
                }
            } catch (Exception e) {
                log.warn("SMS delivery status refresh failed for {} via {}: {}",
                        audit.getMessageId(), audit.getProvider(), e.getMessage());
            }
        }
    }
}
