package com.trackpro.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "sms_audit_log")
public class SmsAuditLog {
    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "message_id", length = 100)
    private String messageId;

    @Column(nullable = false, length = 10)
    private String direction;

    @Column(nullable = false, length = 30)
    private String provider;

    @Column(name = "from_number", length = 30)
    private String fromNumber;

    @Column(name = "to_number", length = 30)
    private String toNumber;

    @Column(name = "message_body", columnDefinition = "TEXT")
    private String messageBody;

    @Column(length = 20)
    private String status;

    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt = Instant.now();

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "related_imei", length = 20)
    private String relatedImei;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getFromNumber() { return fromNumber; }
    public void setFromNumber(String fromNumber) { this.fromNumber = fromNumber; }

    public String getToNumber() { return toNumber; }
    public void setToNumber(String toNumber) { this.toNumber = toNumber; }

    public String getMessageBody() { return messageBody; }
    public void setMessageBody(String messageBody) { this.messageBody = messageBody; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRawResponse() { return rawResponse; }
    public void setRawResponse(String rawResponse) { this.rawResponse = rawResponse; }

    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }

    public Instant getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(Instant deliveredAt) { this.deliveredAt = deliveredAt; }

    public String getRelatedImei() { return relatedImei; }
    public void setRelatedImei(String relatedImei) { this.relatedImei = relatedImei; }
}
