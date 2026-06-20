create table organisations (
  id          uuid primary key,
  name        varchar(200) not null,
  slug        varchar(120) not null unique,
  plan        varchar(32)  not null default 'Starter',
  status      varchar(32)  not null default 'Active',
  admin_email varchar(320) not null,
  vehicle_limit int        not null default 10,
  created_at  timestamp    not null default now(),
  updated_at  timestamp    not null default now()
);

create index idx_organisations_slug on organisations(slug);
create index idx_organisations_status on organisations(status);

alter table users
  add column if not exists organisation_id uuid references organisations(id) on delete set null;

create index idx_users_organisation_id on users(organisation_id);
