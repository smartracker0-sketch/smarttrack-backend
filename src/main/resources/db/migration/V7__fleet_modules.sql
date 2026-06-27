-- ============================================================
-- V7: Fleet modules — maintenance, expenses, documents,
--     vendors, consigners, elock, tyre management
-- ============================================================

-- ── Maintenance records ─────────────────────────────────────
CREATE TABLE IF NOT EXISTS maintenance_records (
    id              UUID PRIMARY KEY,
    organisation_id UUID REFERENCES organisations(id) ON DELETE CASCADE,
    device_id       UUID REFERENCES devices(id) ON DELETE SET NULL,
    vehicle_plate   VARCHAR(32),
    task            VARCHAR(200) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING','IN_PROGRESS','DONE','OVERDUE')),
    due_date        DATE,
    completed_at    TIMESTAMP WITH TIME ZONE,
    cost            NUMERIC(12,2),
    notes           TEXT,
    created_by      UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_maintenance_org ON maintenance_records(organisation_id, due_date DESC);
CREATE INDEX IF NOT EXISTS idx_maintenance_device ON maintenance_records(device_id);

-- ── Expenses ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS expenses (
    id              UUID PRIMARY KEY,
    organisation_id UUID REFERENCES organisations(id) ON DELETE CASCADE,
    device_id       UUID REFERENCES devices(id) ON DELETE SET NULL,
    vehicle_plate   VARCHAR(32),
    category        VARCHAR(50) NOT NULL DEFAULT 'OTHER'
                        CHECK (category IN ('FUEL','TOLL','MAINTENANCE','TYRE','INSURANCE','OTHER')),
    amount          NUMERIC(12,2) NOT NULL,
    currency        VARCHAR(10) NOT NULL DEFAULT 'USD',
    description     VARCHAR(500),
    expense_date    DATE NOT NULL,
    created_by      UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_expenses_org ON expenses(organisation_id, expense_date DESC);
CREATE INDEX IF NOT EXISTS idx_expenses_device ON expenses(device_id);

-- ── Documents ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS documents (
    id              UUID PRIMARY KEY,
    organisation_id UUID REFERENCES organisations(id) ON DELETE CASCADE,
    device_id       UUID REFERENCES devices(id) ON DELETE SET NULL,
    vehicle_plate   VARCHAR(32),
    doc_type        VARCHAR(50) NOT NULL DEFAULT 'OTHER'
                        CHECK (doc_type IN ('INSURANCE','REGISTRATION','PERMIT','INSPECTION','LICENCE','OTHER')),
    title           VARCHAR(200) NOT NULL,
    file_url        VARCHAR(1024),
    expiry_date     DATE,
    status          VARCHAR(20) NOT NULL DEFAULT 'VALID'
                        CHECK (status IN ('VALID','EXPIRING_SOON','EXPIRED')),
    created_by      UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_documents_org ON documents(organisation_id, expiry_date ASC);
CREATE INDEX IF NOT EXISTS idx_documents_device ON documents(device_id);

-- ── Vendors ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS vendors (
    id              UUID PRIMARY KEY,
    organisation_id UUID REFERENCES organisations(id) ON DELETE CASCADE,
    name            VARCHAR(200) NOT NULL,
    vendor_type     VARCHAR(50) NOT NULL DEFAULT 'OTHER'
                        CHECK (vendor_type IN ('FUEL','MAINTENANCE','TYRES','INSURANCE','CCTV','OTHER')),
    contact_name    VARCHAR(120),
    contact_phone   VARCHAR(30),
    contact_email   VARCHAR(320),
    address         TEXT,
    active          BOOLEAN NOT NULL DEFAULT true,
    created_by      UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_vendors_org ON vendors(organisation_id);

-- ── Consigners ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS consigners (
    id              UUID PRIMARY KEY,
    organisation_id UUID REFERENCES organisations(id) ON DELETE CASCADE,
    name            VARCHAR(200) NOT NULL,
    consigner_type  VARCHAR(20) NOT NULL DEFAULT 'CONSIGNOR'
                        CHECK (consigner_type IN ('CONSIGNOR','CONSIGNEE')),
    contact_phone   VARCHAR(30),
    contact_email   VARCHAR(320),
    address         TEXT,
    active          BOOLEAN NOT NULL DEFAULT true,
    created_by      UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_consigners_org ON consigners(organisation_id);

-- ── E-Lock events ───────────────────────────────────────────
CREATE TABLE IF NOT EXISTS elock_devices (
    id              UUID PRIMARY KEY,
    organisation_id UUID REFERENCES organisations(id) ON DELETE CASCADE,
    device_id       UUID REFERENCES devices(id) ON DELETE CASCADE,
    lock_serial     VARCHAR(100) NOT NULL UNIQUE,
    status          VARCHAR(20) NOT NULL DEFAULT 'LOCKED'
                        CHECK (status IN ('LOCKED','UNLOCKED','ERROR','UNKNOWN')),
    last_event_at   TIMESTAMP WITH TIME ZONE,
    notes           TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_elock_org ON elock_devices(organisation_id);
CREATE INDEX IF NOT EXISTS idx_elock_device ON elock_devices(device_id);

-- ── Tyre records ────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS tyre_records (
    id              UUID PRIMARY KEY,
    organisation_id UUID REFERENCES organisations(id) ON DELETE CASCADE,
    device_id       UUID REFERENCES devices(id) ON DELETE SET NULL,
    vehicle_plate   VARCHAR(32),
    position        VARCHAR(50) NOT NULL,
    brand           VARCHAR(100),
    serial_no       VARCHAR(100),
    status          VARCHAR(20) NOT NULL DEFAULT 'GOOD'
                        CHECK (status IN ('GOOD','WORN','REPLACE','DAMAGED')),
    pressure_psi    NUMERIC(5,1),
    installed_at    DATE,
    next_check_date DATE,
    notes           TEXT,
    created_by      UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_tyre_org ON tyre_records(organisation_id);
CREATE INDEX IF NOT EXISTS idx_tyre_device ON tyre_records(device_id);

-- ── Driver profiles (extends existing users + score columns) ─
-- Add licence and phone fields to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone        VARCHAR(30);
ALTER TABLE users ADD COLUMN IF NOT EXISTS licence_no   VARCHAR(80);
ALTER TABLE users ADD COLUMN IF NOT EXISTS licence_expiry DATE;
