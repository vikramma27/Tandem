
create extension if not exists pgcrypto;

create table public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  display_name text not null default '',
  avatar_url text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.couples (
  id uuid primary key default gen_random_uuid(),
  created_by uuid not null references auth.users(id) on delete cascade,
  name text not null default 'Our space',
  created_at timestamptz not null default now()
);

create table public.pair_members (
  pair_id uuid not null references public.couples(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  role text not null default 'member' check (role in ('owner','member')),
  joined_at timestamptz not null default now(),
  primary key (pair_id,user_id),
  unique (user_id)
);

create table public.pair_invites (
  id uuid primary key default gen_random_uuid(),
  pair_id uuid not null references public.couples(id) on delete cascade,
  created_by uuid not null references auth.users(id) on delete cascade,
  code_hash text not null unique,
  expires_at timestamptz not null default (now() + interval '48 hours'),
  accepted_by uuid references auth.users(id) on delete set null,
  accepted_at timestamptz,
  created_at timestamptz not null default now()
);

create table public.travel_plans (
  id uuid primary key default gen_random_uuid(),
  pair_id uuid not null references public.couples(id) on delete cascade,
  created_by uuid not null references auth.users(id) on delete cascade,
  recipient_user_id uuid references auth.users(id) on delete set null,
  title text not null,
  destination text not null default '',
  departure_at timestamptz not null,
  reminder_at timestamptz,
  timezone text not null default 'Asia/Kolkata',
  notes text not null default '',
  status text not null default 'planned' check (status in ('planned','departed','cancelled')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.travel_checklist_items (
  id uuid primary key default gen_random_uuid(),
  plan_id uuid not null references public.travel_plans(id) on delete cascade,
  title text not null,
  sort_order integer not null default 0,
  is_done boolean not null default false,
  completed_by uuid references auth.users(id) on delete set null,
  completed_at timestamptz,
  updated_at timestamptz not null default now()
);

create table public.shared_tasks (
  id uuid primary key,
  pair_id uuid not null references public.couples(id) on delete cascade,
  space_id text not null default 'default',
  list_id text not null default 'default',
  title text not null,
  note text not null default '',
  mode text not null default 'TODO',
  created_by uuid not null references auth.users(id) on delete cascade,
  assigned_to uuid references auth.users(id) on delete set null,
  urgency text not null default 'SOON',
  category text not null default 'General',
  quantity text not null default '1',
  unit text not null default 'pc',
  aisle text not null default 'PRODUCE',
  estimated_cost numeric(12,2) not null default 0,
  store_tag text not null default '',
  is_completed boolean not null default false,
  completed_by uuid references auth.users(id) on delete set null,
  completed_at timestamptz,
  reaction_chip text,
  due_label text not null default '',
  updated_at timestamptz not null default now(),
  created_at timestamptz not null default now()
);

create table public.partner_nudges (
  id uuid primary key,
  pair_id uuid not null references public.couples(id) on delete cascade,
  sender_user_id uuid not null references auth.users(id) on delete cascade,
  receiver_user_id uuid not null references auth.users(id) on delete cascade,
  message text not null,
  linked_task_id uuid references public.shared_tasks(id) on delete set null,
  urgency text not null default 'URGENT_PING',
  is_acknowledged boolean not null default false,
  reply_chip text,
  created_at timestamptz not null default now(),
  acknowledged_at timestamptz
);

create table public.rituals (
  id uuid primary key,
  pair_id uuid not null references public.couples(id) on delete cascade,
  title text not null,
  time_window text not null default '',
  created_by uuid not null references auth.users(id) on delete cascade,
  created_at timestamptz not null default now()
);

create table public.ritual_checkins (
  id uuid primary key default gen_random_uuid(),
  ritual_id uuid not null references public.rituals(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  checkin_date date not null default (now() at time zone 'Asia/Kolkata')::date,
  completed_at timestamptz not null default now(),
  unique (ritual_id,user_id,checkin_date)
);

create table public.device_tokens (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  fcm_token text not null unique,
  platform text not null default 'android',
  device_label text not null default '',
  last_seen_at timestamptz not null default now(),
  created_at timestamptz not null default now()
);

create table public.notification_events (
  id uuid primary key default gen_random_uuid(),
  pair_id uuid not null references public.couples(id) on delete cascade,
  recipient_user_id uuid not null references auth.users(id) on delete cascade,
  actor_user_id uuid references auth.users(id) on delete set null,
  event_type text not null,
  title text not null,
  body text not null,
  payload jsonb not null default '{}'::jsonb,
  scheduled_for timestamptz not null default now(),
  delivered_at timestamptz,
  read_at timestamptz,
  created_at timestamptz not null default now()
);

create index travel_plans_pair_departure_idx on public.travel_plans(pair_id, departure_at);
create index travel_checklist_plan_order_idx on public.travel_checklist_items(plan_id, sort_order);
create index shared_tasks_pair_updated_idx on public.shared_tasks(pair_id, updated_at desc);
create index partner_nudges_receiver_created_idx on public.partner_nudges(receiver_user_id, created_at desc);
create index notification_events_recipient_schedule_idx on public.notification_events(recipient_user_id, scheduled_for);
create index ritual_checkins_user_date_idx on public.ritual_checkins(user_id, checkin_date desc);

create or replace function public.handle_new_user()
returns trigger language plpgsql security definer set search_path = public
as $$
begin
  insert into public.profiles(id, display_name)
  values (new.id, coalesce(new.raw_user_meta_data->>'display_name',''))
  on conflict (id) do nothing;
  return new;
end;
$$;

drop trigger if exists on_auth_user_created_tandem on auth.users;
create trigger on_auth_user_created_tandem after insert on auth.users
for each row execute procedure public.handle_new_user();

create or replace function public.create_pair(pair_name text default 'Our space')
returns uuid language plpgsql security definer set search_path = public
as $$
declare new_pair_id uuid;
begin
  if auth.uid() is null then raise exception 'Authentication required'; end if;
  if exists(select 1 from public.pair_members where user_id = auth.uid()) then
    raise exception 'This account is already paired';
  end if;
  insert into public.couples(created_by,name) values(auth.uid(),coalesce(nullif(trim(pair_name),''),'Our space'))
  returning id into new_pair_id;
  insert into public.pair_members(pair_id,user_id,role) values(new_pair_id,auth.uid(),'owner');
  return new_pair_id;
end;
$$;

create or replace function public.create_pair_invite(pair_id_input uuid)
returns table(invite_code text, expires_at timestamptz)
language plpgsql security definer set search_path = public
as $$
declare raw_code text;
declare expiry timestamptz;
begin
  if auth.uid() is null or not exists(
    select 1 from public.pair_members where pair_id = pair_id_input and user_id = auth.uid()
  ) then raise exception 'Not a member of this pair'; end if;
  if (select count(*) from public.pair_members where pair_id = pair_id_input) >= 2 then
    raise exception 'This pair already has two members';
  end if;
  raw_code := upper(encode(gen_random_bytes(4),'hex'));
  expiry := now() + interval '48 hours';
  insert into public.pair_invites(pair_id,created_by,code_hash,expires_at)
  values(pair_id_input,auth.uid(),encode(digest(raw_code,'sha256'),'hex'),expiry);
  return query select raw_code, expiry;
end;
$$;

create or replace function public.accept_pair_invite(invite_code_input text)
returns uuid language plpgsql security definer set search_path = public
as $$
declare target_invite public.pair_invites%rowtype;
declare members_count integer;
begin
  if auth.uid() is null then raise exception 'Authentication required'; end if;
  if exists(select 1 from public.pair_members where user_id = auth.uid()) then
    raise exception 'This account is already paired';
  end if;
  select * into target_invite from public.pair_invites
  where code_hash = encode(digest(upper(regexp_replace(trim(invite_code_input),'[^A-Z0-9]','','g')),'sha256'),'hex')
    and accepted_at is null and expires_at > now()
  for update;
  if not found then raise exception 'Invite is invalid or expired'; end if;
  select count(*) into members_count from public.pair_members where pair_id = target_invite.pair_id;
  if members_count >= 2 then raise exception 'This pair already has two members'; end if;
  insert into public.pair_members(pair_id,user_id,role) values(target_invite.pair_id,auth.uid(),'member');
  update public.pair_invites set accepted_by=auth.uid(),accepted_at=now() where id=target_invite.id;
  return target_invite.pair_id;
end;
$$;

grant execute on function public.create_pair(text) to authenticated;
grant execute on function public.create_pair_invite(uuid) to authenticated;
grant execute on function public.accept_pair_invite(text) to authenticated;

alter table public.profiles enable row level security;
alter table public.couples enable row level security;
alter table public.pair_members enable row level security;
alter table public.pair_invites enable row level security;
alter table public.travel_plans enable row level security;
alter table public.travel_checklist_items enable row level security;
alter table public.shared_tasks enable row level security;
alter table public.partner_nudges enable row level security;
alter table public.rituals enable row level security;
alter table public.ritual_checkins enable row level security;
alter table public.device_tokens enable row level security;
alter table public.notification_events enable row level security;

create policy "profiles_select_self_or_partner" on public.profiles for select to authenticated
using (id = auth.uid() or exists (
  select 1 from public.pair_members mine join public.pair_members theirs on theirs.pair_id=mine.pair_id
  where mine.user_id=auth.uid() and theirs.user_id=profiles.id
));
create policy "profiles_update_self" on public.profiles for update to authenticated using(id=auth.uid()) with check(id=auth.uid());

create policy "couples_select_members" on public.couples for select to authenticated
using (exists(select 1 from public.pair_members pm where pm.pair_id=couples.id and pm.user_id=auth.uid()));
create policy "pair_members_select_pair" on public.pair_members for select to authenticated
using (exists(select 1 from public.pair_members mine where mine.pair_id=pair_members.pair_id and mine.user_id=auth.uid()));
create policy "pair_invites_select_creator" on public.pair_invites for select to authenticated
using (created_by=auth.uid());

create policy "travel_plans_select_pair" on public.travel_plans for select to authenticated
using (exists(select 1 from public.pair_members pm where pm.pair_id=travel_plans.pair_id and pm.user_id=auth.uid()));
create policy "travel_plans_insert_member" on public.travel_plans for insert to authenticated
with check (created_by=auth.uid() and exists(select 1 from public.pair_members pm where pm.pair_id=travel_plans.pair_id and pm.user_id=auth.uid()));
create policy "travel_plans_update_member" on public.travel_plans for update to authenticated
using (exists(select 1 from public.pair_members pm where pm.pair_id=travel_plans.pair_id and pm.user_id=auth.uid()))
with check (exists(select 1 from public.pair_members pm where pm.pair_id=travel_plans.pair_id and pm.user_id=auth.uid()));
create policy "travel_plans_delete_member" on public.travel_plans for delete to authenticated
using (exists(select 1 from public.pair_members pm where pm.pair_id=travel_plans.pair_id and pm.user_id=auth.uid()));

create policy "travel_items_access_via_plan" on public.travel_checklist_items for all to authenticated
using (exists(select 1 from public.travel_plans tp join public.pair_members pm on pm.pair_id=tp.pair_id where tp.id=travel_checklist_items.plan_id and pm.user_id=auth.uid()))
with check (exists(select 1 from public.travel_plans tp join public.pair_members pm on pm.pair_id=tp.pair_id where tp.id=travel_checklist_items.plan_id and pm.user_id=auth.uid()));

create policy "shared_tasks_pair_access" on public.shared_tasks for all to authenticated
using (exists(select 1 from public.pair_members pm where pm.pair_id=shared_tasks.pair_id and pm.user_id=auth.uid()))
with check (created_by=auth.uid() or exists(select 1 from public.pair_members pm where pm.pair_id=shared_tasks.pair_id and pm.user_id=auth.uid()));

create policy "nudges_pair_access" on public.partner_nudges for all to authenticated
using (exists(select 1 from public.pair_members pm where pm.pair_id=partner_nudges.pair_id and pm.user_id=auth.uid()))
with check (sender_user_id=auth.uid() and exists(select 1 from public.pair_members pm where pm.pair_id=partner_nudges.pair_id and pm.user_id=auth.uid()));

create policy "rituals_pair_access" on public.rituals for all to authenticated
using (exists(select 1 from public.pair_members pm where pm.pair_id=rituals.pair_id and pm.user_id=auth.uid()))
with check (exists(select 1 from public.pair_members pm where pm.pair_id=rituals.pair_id and pm.user_id=auth.uid()));

create policy "ritual_checkins_pair_access" on public.ritual_checkins for all to authenticated
using (user_id=auth.uid() or exists(
 select 1 from public.rituals r join public.pair_members pm on pm.pair_id=r.pair_id
 where r.id=ritual_checkins.ritual_id and pm.user_id=auth.uid()
))
with check (user_id=auth.uid() and exists(
 select 1 from public.rituals r join public.pair_members pm on pm.pair_id=r.pair_id
 where r.id=ritual_checkins.ritual_id and pm.user_id=auth.uid()
));

create policy "device_tokens_owner_access" on public.device_tokens for all to authenticated
using(user_id=auth.uid()) with check(user_id=auth.uid());

create policy "notifications_recipient_access" on public.notification_events for select to authenticated
using(recipient_user_id=auth.uid());
create policy "notifications_update_read" on public.notification_events for update to authenticated
using(recipient_user_id=auth.uid()) with check(recipient_user_id=auth.uid());

do $$
begin
  alter publication supabase_realtime add table public.travel_plans;
exception when duplicate_object then null;
when undefined_object then null;
end $$;
do $$
begin
  alter publication supabase_realtime add table public.travel_checklist_items;
exception when duplicate_object then null;
when undefined_object then null;
end $$;
do $$
begin
  alter publication supabase_realtime add table public.shared_tasks;
exception when duplicate_object then null;
when undefined_object then null;
end $$;
do $$
begin
  alter publication supabase_realtime add table public.partner_nudges;
exception when duplicate_object then null;
when undefined_object then null;
end $$;
do $$
begin
  alter publication supabase_realtime add table public.notification_events;
exception when duplicate_object then null;
when undefined_object then null;
end $$;
