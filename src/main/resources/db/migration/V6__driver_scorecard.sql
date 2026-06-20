-- Driver scorecard schema additions

-- Trips table (driver scorecard grouping unit)
CREATE TABLE IF NOT EXISTS trips (
    id UUID PRIMARY KEY,
    driver_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ended_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS' CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'ABANDONED')),
    last_location_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_trips_driver ON trips(driver_id, started_at DESC);
CREATE INDEX IF NOT EXISTS idx_trips_status ON trips(status, last_location_at);

-- Per-trip driver score record
CREATE TABLE IF NOT EXISTS driver_scores (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    driver_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    org_id UUID REFERENCES organisations(id) ON DELETE SET NULL,
    score INT NOT NULL,
    breakdown TEXT,
    clean_trip_bonus BOOLEAN NOT NULL DEFAULT FALSE,
    streak_bonus BOOLEAN NOT NULL DEFAULT FALSE,
    adjusted_score INT,
    adjustment_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_driver_scores_driver_created ON driver_scores(driver_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_driver_scores_org ON driver_scores(org_id, score DESC);

-- Add rolling score fields to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS score_total DECIMAL(5,2) DEFAULT 100.00;
ALTER TABLE users ADD COLUMN IF NOT EXISTS score_band VARCHAR(20) DEFAULT 'EXCELLENT';
ALTER TABLE users ADD COLUMN IF NOT EXISTS total_trips_scored INT DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_scored_at TIMESTAMP WITH TIME ZONE;
