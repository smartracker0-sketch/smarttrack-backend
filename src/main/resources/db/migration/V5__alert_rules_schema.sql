-- Alert rule engine schema additions

-- Geofences table
CREATE TABLE IF NOT EXISTS geofences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id UUID NOT NULL REFERENCES organisations(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    geofence_type VARCHAR(32) NOT NULL CHECK (geofence_type IN ('CIRCLE', 'POLYGON')),
    severity VARCHAR(32) NOT NULL DEFAULT 'LOW' CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH')),
    geometry_json TEXT NOT NULL,
    center_lat DOUBLE PRECISION,
    center_lng DOUBLE PRECISION,
    radius_m DOUBLE PRECISION,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_geofences_organisation ON geofences(organisation_id);
CREATE INDEX idx_geofences_active ON geofences(organisation_id, active);

-- Extend devices with per-vehicle speed limit override
ALTER TABLE devices ADD COLUMN IF NOT EXISTS speed_limit_kmh INTEGER;

-- Extend organisations with default alert thresholds
ALTER TABLE organisations ADD COLUMN IF NOT EXISTS default_speed_limit_kmh INTEGER;
ALTER TABLE organisations ADD COLUMN IF NOT EXISTS idle_threshold_minutes INTEGER;
ALTER TABLE organisations ADD COLUMN IF NOT EXISTS idle_escalation_minutes INTEGER;

-- Extend device_alerts with new alert rule engine fields
ALTER TABLE device_alerts
    ADD COLUMN IF NOT EXISTS speed_kph DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS duration_seconds BIGINT,
    ADD COLUMN IF NOT EXISTS related_geofence_id UUID REFERENCES geofences(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS metadata TEXT;

-- Ensure existing alert_type/severity values are VARCHAR (enums stored as strings)
ALTER TABLE device_alerts ALTER COLUMN alert_type TYPE VARCHAR(64);
ALTER TABLE device_alerts ALTER COLUMN severity TYPE VARCHAR(32);

CREATE INDEX idx_device_alerts_type ON device_alerts(device_id, alert_type);
CREATE INDEX idx_device_alerts_unack ON device_alerts(device_id, alert_type, acknowledged);
