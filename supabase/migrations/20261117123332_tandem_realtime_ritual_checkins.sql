
alter table public.rituals add column if not exists updated_at timestamptz not null default now();

create or replace function private.bump_ritual_updated_at()
returns trigger language plpgsql security definer set search_path=public
as $$
declare target_ritual_id text;
begin
  target_ritual_id := case when tg_op='DELETE' then old.ritual_id else new.ritual_id end;
  update public.rituals set updated_at=now() where id=target_ritual_id;
  if tg_op='DELETE' then return old; else return new; end if;
end;
$$;
drop trigger if exists ritual_checkins_bump_parent on public.ritual_checkins;
create trigger ritual_checkins_bump_parent
after insert or delete on public.ritual_checkins
for each row execute procedure private.bump_ritual_updated_at();

do $$ begin alter publication supabase_realtime add table public.rituals; exception when duplicate_object then null; when undefined_object then null; end $$;
