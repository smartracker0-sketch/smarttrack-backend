alter table devices add column if not exists organisation_id uuid references organisations(id) on delete set null;
alter table devices add column if not exists device_type     varchar(64)  not null default 'GPS Tracker';
alter table devices add column if not exists firmware        varchar(32)  not null default 'unknown';
alter table devices add column if not exists sim_card        varchar(64);
alter table devices add column if not exists serial_no       varchar(64);
alter table devices add column if not exists vehicle_plate   varchar(64);
alter table devices add column if not exists status          varchar(32)  not null default 'Unassigned';
alter table devices add column if not exists notes           text;

create index if not exists idx_devices_organisation_id on devices(organisation_id);
create index if not exists idx_devices_status on devices(status);

alter table devices alter column owner_user_id drop not null;
