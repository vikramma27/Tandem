-- Tandem initial backend schema.
-- Apply once to a fresh Supabase project. Client access is authenticated + RLS only.
create extension if not exists pgcrypto with schema extensions;
create schema if not exists private;
revoke all on schema private from public, anon;
grant usage on schema private to authenticated;

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
  id text primary key,
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
  id text primary key,
  pair_id uuid not null references public.couples(id) on delete cascade,
  sender_user_id uuid not null references auth.users(id) on delete cascade,
  receiver_user_id uuid not null references auth.users(id) on delete cascade,
  message text not null,
  linked_task_id text references public.shared_tasks(id) on delete set null,
  urgency text not null default 'URGENT_PING',
  is_acknowledged boolean not null default false,
  reply_chip text,
  created_at timestamptz not null default now(),
  acknowledged_at timestamptz
);
create table public.rituals (
  id text primary key,
  pair_id uuid not null references public.couples(id) on delete cascade,
  title text not null,
  time_window text not null default '',
  created_by uuid not null references auth.users(id) on delete cascade,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create table public.ritual_checkins (
  id uuid primary key default gen_random_uuid(),
  ritual_id text not null references public.rituals(id) on delete cascade,
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

create index travel_plans_pair_departure_idx on public.travel_plans(pair_id,departure_at);
create index travel_plans_created_by_idx on public.travel_plans(created_by);
create index travel_plans_recipient_idx on public.travel_plans(recipient_user_id);
create index travel_checklist_plan_order_idx on public.travel_checklist_items(plan_id,sort_order);
create index shared_tasks_pair_updated_idx on public.shared_tasks(pair_id,updated_at desc);
create index shared_tasks_created_by_idx on public.shared_tasks(created_by);
create index shared_tasks_assigned_to_idx on public.shared_tasks(assigned_to);
create index partner_nudges_receiver_created_idx on public.partner_nudges(receiver_user_id,created_at desc);
create index partner_nudges_pair_idx on public.partner_nudges(pair_id);
create index rituals_pair_idx on public.rituals(pair_id);
create index ritual_checkins_user_date_idx on public.ritual_checkins(user_id,checkin_date desc);
create index device_tokens_user_idx on public.device_tokens(user_id);
create index notification_events_recipient_schedule_idx on public.notification_events(recipient_user_id,scheduled_for);
create index notification_events_pair_idx on public.notification_events(pair_id);

create or replace function private.is_pair_member(target_pair_id uuid,target_user_id uuid default auth.uid())
returns boolean language sql stable security definer set search_path=public
as $$ select exists(select 1 from public.pair_members where pair_id=target_pair_id and user_id=target_user_id); $$;
grant execute on function private.is_pair_member(uuid,uuid) to authenticated;

create or replace function private.create_pair_impl(pair_name text default 'Our space')
returns uuid language plpgsql security definer set search_path=public
as $$
declare new_pair_id uuid;
begin
  if auth.uid() is null then raise exception 'Authentication required'; end if;
  if exists(select 1 from public.pair_members where user_id=auth.uid()) then raise exception 'This account is already paired'; end if;
  insert into public.couples(created_by,name) values(auth.uid(),coalesce(nullif(trim(pair_name),''),'Our space')) returning id into new_pair_id;
  insert into public.pair_members(pair_id,user_id,role) values(new_pair_id,auth.uid(),'owner');
  return new_pair_id;
end;
$$;
create or replace function private.create_pair_invite_impl(pair_id_input uuid)
returns table(invite_code text,expires_at timestamptz)
language plpgsql security definer set search_path=public
as $$
declare raw_code text;
declare expiry timestamptz;
begin
  if auth.uid() is null or not exists(select 1 from public.pair_members where pair_id=pair_id_input and user_id=auth.uid()) then raise exception 'Not a member of this pair'; end if;
  if (select count(*) from public.pair_members where pair_id=pair_id_input)>=2 then raise exception 'This pair already has two members'; end if;
  update public.pair_invites pi set expires_at=now() where pi.pair_id=pair_id_input and pi.accepted_at is null and pi.expires_at>now();
  raw_code := upper(encode(extensions.gen_random_bytes(8),'hex'));
  expiry := now()+interval '48 hours';
  insert into public.pair_invites(pair_id,created_by,code_hash,expires_at)
  values(pair_id_input,auth.uid(),encode(extensions.digest(raw_code::bytea,'sha256'),'hex'),expiry);
  return query select raw_code,expiry;
end;
$$;
create or replace function private.accept_pair_invite_impl(invite_code_input text)
returns uuid language plpgsql security definer set search_path=public
as $$
declare target_invite public.pair_invites%rowtype;
declare members_count integer;
declare normalized_code text;
begin
  if auth.uid() is null then raise exception 'Authentication required'; end if;
  if exists(select 1 from public.pair_members where user_id=auth.uid()) then raise exception 'This account is already paired'; end if;
  normalized_code := regexp_replace(upper(trim(invite_code_input)),'[^A-Z0-9]','','g');
  select * into target_invite from public.pair_invites
  where code_hash=encode(extensions.digest(normalized_code::bytea,'sha256'),'hex')
    and accepted_at is null and expires_at>now() for update;
  if not found then raise exception 'Invite is invalid or expired'; end if;
  select count(*) into members_count from public.pair_members where pair_id=target_invite.pair_id;
  if members_count>=2 then raise exception 'This pair already has two members'; end if;
  insert into public.pair_members(pair_id,user_id,role) values(target_invite.pair_id,auth.uid(),'member');
  update public.pair_invites set accepted_by=auth.uid(),accepted_at=now() where id=target_invite.id;
  return target_invite.pair_id;
end;
$$;

revoke execute on function private.create_pair_impl(text) from public,anon;
revoke execute on function private.create_pair_invite_impl(uuid) from public,anon;
revoke execute on function private.accept_pair_invite_impl(text) from public,anon;
grant execute on function private.create_pair_impl(text) to authenticated;
grant execute on function private.create_pair_invite_impl(uuid) to authenticated;
grant execute on function private.accept_pair_invite_impl(text) to authenticated;

create or replace function public.create_pair(pair_name text default 'Our space')
returns uuid language sql security invoker set search_path=public,private
as $$ select private.create_pair_impl(pair_name); $$;
create or replace function public.create_pair_invite(pair_id_input uuid)
returns table(invite_code text,expires_at timestamptz)
language sql security invoker set search_path=public,private
as $$ select * from private.create_pair_invite_impl(pair_id_input); $$;
create or replace function public.accept_pair_invite(invite_code_input text)
returns uuid language sql security invoker set search_path=public,private
as $$ select private.accept_pair_invite_impl(invite_code_input); $$;
revoke execute on function public.create_pair(text) from public,anon;
revoke execute on function public.create_pair_invite(uuid) from public,anon;
revoke execute on function public.accept_pair_invite(text) from public,anon;
grant execute on function public.create_pair(text) to authenticated;
grant execute on function public.create_pair_invite(uuid) to authenticated;
grant execute on function public.accept_pair_invite(text) to authenticated;

create or replace function private.handle_new_user()
returns trigger language plpgsql security definer set search_path=public
as $$
begin
  insert into public.profiles(id,display_name) values(new.id,coalesce(new.raw_user_meta_data->>'display_name','')) on conflict(id) do nothing;
  return new;
end;
$$;
revoke execute on function private.handle_new_user() from public,anon,authenticated;
drop trigger if exists on_auth_user_created_tandem on auth.users;
create trigger on_auth_user_created_tandem after insert on auth.users for each row execute procedure private.handle_new_user();

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

create policy profiles_select_self_or_partner on public.profiles for select to authenticated
using(id=(select auth.uid()) or exists(
  select 1 from public.pair_members mine join public.pair_members theirs on theirs.pair_id=mine.pair_id
  where mine.user_id=(select auth.uid()) and theirs.user_id=profiles.id
));
create policy profiles_update_self on public.profiles for update to authenticated
using(id=(select auth.uid())) with check(id=(select auth.uid()));
create policy couples_select_members on public.couples for select to authenticated
using(private.is_pair_member(id,(select auth.uid())));
create policy pair_members_select_pair on public.pair_members for select to authenticated
using(private.is_pair_member(pair_id,(select auth.uid())));
create policy pair_invites_select_creator on public.pair_invites for select to authenticated
using(created_by=(select auth.uid()));

create policy travel_plans_select_pair on public.travel_plans for select to authenticated
using(private.is_pair_member(pair_id,(select auth.uid())));
create policy travel_plans_insert_member on public.travel_plans for insert to authenticated
with check(created_by=(select auth.uid()) and private.is_pair_member(pair_id,(select auth.uid()))
  and (recipient_user_id is null or private.is_pair_member(pair_id,recipient_user_id)));
create policy travel_plans_update_member on public.travel_plans for update to authenticated
using(private.is_pair_member(pair_id,(select auth.uid())))
with check(private.is_pair_member(pair_id,(select auth.uid()))
  and (recipient_user_id is null or private.is_pair_member(pair_id,recipient_user_id)));
create policy travel_plans_delete_member on public.travel_plans for delete to authenticated
using(private.is_pair_member(pair_id,(select auth.uid())));
create policy travel_items_access_via_plan on public.travel_checklist_items for all to authenticated
using(exists(select 1 from public.travel_plans tp where tp.id=travel_checklist_items.plan_id and private.is_pair_member(tp.pair_id,(select auth.uid()))))
with check(exists(select 1 from public.travel_plans tp where tp.id=travel_checklist_items.plan_id and private.is_pair_member(tp.pair_id,(select auth.uid()))));

create policy shared_tasks_pair_access on public.shared_tasks for all to authenticated
using(private.is_pair_member(pair_id,(select auth.uid())))
with check(private.is_pair_member(pair_id,(select auth.uid())) and (created_by=(select auth.uid()) or private.is_pair_member(pair_id,created_by)));
create policy nudges_pair_select on public.partner_nudges for select to authenticated
using(private.is_pair_member(pair_id,(select auth.uid())));
create policy nudges_insert_sender on public.partner_nudges for insert to authenticated
with check(sender_user_id=(select auth.uid()) and private.is_pair_member(pair_id,(select auth.uid())) and private.is_pair_member(pair_id,receiver_user_id));
create policy nudges_update_member on public.partner_nudges for update to authenticated
using(private.is_pair_member(pair_id,(select auth.uid())))
with check(private.is_pair_member(pair_id,(select auth.uid())));
create policy nudges_delete_sender on public.partner_nudges for delete to authenticated
using(sender_user_id=(select auth.uid()));
create policy rituals_pair_access on public.rituals for all to authenticated
using(private.is_pair_member(pair_id,(select auth.uid())))
with check(private.is_pair_member(pair_id,(select auth.uid())));
create policy ritual_checkins_pair_access on public.ritual_checkins for all to authenticated
using(user_id=(select auth.uid()) or exists(
  select 1 from public.rituals r where r.id=ritual_checkins.ritual_id
  and private.is_pair_member(r.pair_id,(select auth.uid()))
))
with check(user_id=(select auth.uid()) and exists(
  select 1 from public.rituals r where r.id=ritual_checkins.ritual_id
  and private.is_pair_member(r.pair_id,(select auth.uid()))
));
create policy device_tokens_owner_access on public.device_tokens for all to authenticated
using(user_id=(select auth.uid())) with check(user_id=(select auth.uid()));
create policy notifications_recipient_access on public.notification_events for select to authenticated
using(recipient_user_id=(select auth.uid()));
create policy notifications_update_read on public.notification_events for update to authenticated
using(recipient_user_id=(select auth.uid())) with check(recipient_user_id=(select auth.uid()));

grant usage on schema public to authenticated;
grant select,update on public.profiles to authenticated;
grant select on public.couples,public.pair_members,public.pair_invites to authenticated;
grant select,insert,update,delete on public.travel_plans,public.travel_checklist_items,public.shared_tasks,
  public.partner_nudges,public.rituals,public.ritual_checkins,public.device_tokens,public.notification_events to authenticated;

create or replace function private.queue_travel_plan_notifications()
returns trigger language plpgsql security definer set search_path=public
as $$
begin
  if tg_op='UPDATE' then
    delete from public.notification_events where payload->>'plan_id'=new.id::text
      and recipient_user_id=new.recipient_user_id and delivered_at is null
      and event_type in ('travel_reminder','travel_departure');
  end if;
  if tg_op='INSERT' and new.recipient_user_id is not null and new.recipient_user_id<>new.created_by then
    insert into public.notification_events(pair_id,recipient_user_id,actor_user_id,event_type,title,body,payload,scheduled_for)
    values(new.pair_id,new.recipient_user_id,new.created_by,'travel_plan_shared','A travel plan was shared',
      new.title||' is on the way to your shared plans',jsonb_build_object('plan_id',new.id,'destination',new.destination),now());
  end if;
  if new.recipient_user_id is not null and new.reminder_at is not null and new.reminder_at>now() then
    insert into public.notification_events(pair_id,recipient_user_id,actor_user_id,event_type,title,body,payload,scheduled_for)
    values(new.pair_id,new.recipient_user_id,new.created_by,'travel_reminder','Travel preparation reminder',
      'Time to check the essentials for '||new.title,jsonb_build_object('plan_id',new.id),new.reminder_at);
  end if;
  if new.recipient_user_id is not null and new.departure_at>now() then
    insert into public.notification_events(pair_id,recipient_user_id,actor_user_id,event_type,title,body,payload,scheduled_for)
    values(new.pair_id,new.recipient_user_id,new.created_by,'travel_departure','Departure time',
      'It is time to leave for '||new.title,jsonb_build_object('plan_id',new.id),new.departure_at);
  end if;
  return new;
end;
$$;
create trigger travel_plan_notifications after insert or update of title,destination,departure_at,reminder_at,recipient_user_id,status
on public.travel_plans for each row execute procedure private.queue_travel_plan_notifications();

create or replace function private.bump_ritual_updated_at()
returns trigger language plpgsql security definer set search_path=public
as $
declare target_ritual_id text;
begin
  target_ritual_id := case when tg_op='DELETE' then old.ritual_id else new.ritual_id end;
  update public.rituals set updated_at=now() where id=target_ritual_id;
  if tg_op='DELETE' then return old; else return new; end if;
end;
$;
create trigger ritual_checkins_bump_parent after insert or delete on public.ritual_checkins
for each row execute procedure private.bump_ritual_updated_at();

create or replace function private.queue_nudge_notification()
returns trigger language plpgsql security definer set search_path=public
as $$
begin
  insert into public.notification_events(pair_id,recipient_user_id,actor_user_id,event_type,title,body,payload,scheduled_for)
  values(new.pair_id,new.receiver_user_id,new.sender_user_id,'partner_nudge','A little nudge from your partner',
    new.message,jsonb_build_object('nudge_id',new.id,'linked_task_id',new.linked_task_id),now());
  return new;
end;
$$;
create trigger partner_nudge_notification after insert on public.partner_nudges
for each row execute procedure private.queue_nudge_notification();

do $$ begin alter publication supabase_realtime add table public.travel_plans; exception when duplicate_object then null; when undefined_object then null; end $$;
do $$ begin alter publication supabase_realtime add table public.travel_checklist_items; exception when duplicate_object then null; when undefined_object then null; end $$;
do $$ begin alter publication supabase_realtime add table public.shared_tasks; exception when duplicate_object then null; when undefined_object then null; end $$;
do $ begin alter publication supabase_realtime add table public.partner_nudges; exception when duplicate_object then null; when undefined_object then null; end $;
do $ begin alter publication supabase_realtime add table public.rituals; exception when duplicate_object then null; when undefined_object then null; end $;
do $$ begin alter publication supabase_realtime add table public.notification_events; exception when duplicate_object then null; when undefined_object then null; end $$;
