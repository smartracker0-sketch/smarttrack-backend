CREATE TABLE sms_audit_log (
    id            UUID PRIMARY KEY,
    message_id    VARCHAR(100),
    direction     VARCHAR(10) NOT NULL,
    provider      VARCHAR(30) NOT NULL,
    from_number   VARCHAR(30),
    to_number     VARCHAR(30),
    message_body  TEXT,
    status        VARCHAR(20),
    raw_response  TEXT,
    sent_at       TIMESTAMP NOT NULL DEFAULT now(),
    delivered_at  TIMESTAMP,
    related_imei  VARCHAR(20)
);

CREATE INDEX idx_sms_audit_imei ON sms_audit_log(related_imei, sent_at DESC);
CREATE INDEX idx_sms_audit_to   ON sms_audit_log(to_number, sent_at DESC);
