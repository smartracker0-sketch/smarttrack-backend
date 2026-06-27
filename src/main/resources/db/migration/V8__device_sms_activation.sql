-- V8: SMS-based device activation fields
ALTER TABLE devices
    ADD COLUMN IF NOT EXISTS sim_number          VARCHAR(20),
    ADD COLUMN IF NOT EXISTS sim_apn             VARCHAR(64),
    ADD COLUMN IF NOT EXISTS manufacturer        VARCHAR(64),
    ADD COLUMN IF NOT EXISTS activation_status   VARCHAR(30)  NOT NULL DEFAULT 'UNACTIVATED',
    ADD COLUMN IF NOT EXISTS activation_attempts INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS activation_attempted_at  TIMESTAMP,
    ADD COLUMN IF NOT EXISTS activation_confirmed_at  TIMESTAMP,
    ADD COLUMN IF NOT EXISTS last_sms_reply       TEXT,
    ADD COLUMN IF NOT EXISTS server_configured    BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS apn_configured       BOOLEAN      NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_devices_activation_status
    ON devices (activation_status);
