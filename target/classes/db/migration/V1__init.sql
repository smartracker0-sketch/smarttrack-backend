create table roles (
  id uuid primary key,
  name varchar(32) not null unique
);

create table users (
  id uuid primary key,
  email varchar(320) not null unique,
  password_hash varchar(100) not null,
  display_name varchar(120) not null,
  avatar_url varchar(1024),
  enabled boolean not null default true,
  created_at timestamp not null default now()
);

create table user_roles (
  user_id uuid not null references users(id) on delete cascade,
  role_id uuid not null references roles(id) on delete cascade,
  primary key (user_id, role_id)
);

create table refresh_tokens (
  id uuid primary key,
  user_id uuid not null references users(id) on delete cascade,
  token_hash varchar(64) not null unique,
  expires_at timestamp not null,
  revoked_at timestamp,
  replaced_by_token_hash varchar(64),
  created_at timestamp not null default now()
);

create index idx_refresh_tokens_user_id on refresh_tokens(user_id);

create table devices (
  id uuid primary key,
  owner_user_id uuid not null references users(id) on delete cascade,
  imei varchar(32) not null unique,
  name varchar(120) not null,
  created_at timestamp not null default now()
);

create index idx_devices_owner on devices(owner_user_id);

create table locations (
  id uuid primary key,
  device_id uuid not null references devices(id) on delete cascade,
  latitude double precision not null,
  longitude double precision not null,
  speed_kph double precision,
  heading_deg double precision,
  accuracy_m double precision,
  recorded_at timestamp not null,
  received_at timestamp not null default now()
);

create index idx_locations_device_recorded_at on locations(device_id, recorded_at);
