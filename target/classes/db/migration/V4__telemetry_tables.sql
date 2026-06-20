-- Raw telemetry frame from any device type
create table telemetry_events (
  id             uuid        primary key,
  device_id      uuid        not null references devices(id) on delete cascade,
  event_time     timestamp   not null,
  received_at    timestamp   not null default now(),
  latitude       double precision,
  longitude      double precision,
  altitude_m     double precision,
  speed_kph      double precision,
  heading_deg    double precision,
  accuracy_m     double precision,
  satellites     int,
  ignition       boolean,
  voltage_mv     int,
  gsm_signal     int,
  odometer_m     bigint,
  raw_payload    text
);

create index idx_telemetry_device_time on telemetry_events(device_id, event_time desc);
create index idx_telemetry_received    on telemetry_events(received_at desc);

-- Fuel sensor readings
create table fuel_readings (
  id          uuid        primary key,
  device_id   uuid        not null references devices(id) on delete cascade,
  event_time  timestamp   not null,
  received_at timestamp   not null default now(),
  fuel_level_pct  double precision,
  fuel_liters     double precision,
  temperature_c   double precision,
  tank_id         varchar(32)
);

create index idx_fuel_device_time on fuel_readings(device_id, event_time desc);

-- Dashcam events (harsh driving, collision, etc.)
create table dashcam_events (
  id            uuid        primary key,
  device_id     uuid        not null references devices(id) on delete cascade,
  event_time    timestamp   not null,
  received_at   timestamp   not null default now(),
  event_type    varchar(64) not null,
  severity      varchar(32) not null default 'INFO',
  latitude      double precision,
  longitude     double precision,
  speed_kph     double precision,
  clip_url      varchar(1024),
  thumbnail_url varchar(1024),
  metadata      text
);

create index idx_dashcam_device_time on dashcam_events(device_id, event_time desc);
create index idx_dashcam_type        on dashcam_events(event_type);

-- Device-level alerts (geofence, speeding, low-battery, etc.)
create table device_alerts (
  id          uuid        primary key,
  device_id   uuid        not null references devices(id) on delete cascade,
  alert_time  timestamp   not null,
  received_at timestamp   not null default now(),
  alert_type  varchar(64) not null,
  severity    varchar(32) not null default 'WARNING',
  message     text,
  acknowledged boolean    not null default false,
  ack_at      timestamp,
  ack_by      uuid        references users(id) on delete set null,
  latitude    double precision,
  longitude   double precision
);

create index idx_alerts_device_time on device_alerts(device_id, alert_time desc);
create index idx_alerts_acked       on device_alerts(acknowledged);
