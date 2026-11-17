
drop policy if exists ritual_checkins_pair_access on public.ritual_checkins;
alter table public.shared_tasks drop constraint if exists shared_tasks_pkey cascade;
alter table public.shared_tasks alter column id type text using id::text;
alter table public.shared_tasks add primary key (id);
alter table public.partner_nudges drop constraint if exists partner_nudges_pkey cascade;
alter table public.partner_nudges alter column id type text using id::text;
alter table public.partner_nudges add primary key (id);
alter table public.partner_nudges alter column linked_task_id type text using linked_task_id::text;
alter table public.partner_nudges add constraint partner_nudges_linked_task_id_fkey
  foreign key (linked_task_id) references public.shared_tasks(id) on delete set null;
alter table public.rituals drop constraint if exists rituals_pkey cascade;
alter table public.rituals alter column id type text using id::text;
alter table public.rituals add primary key (id);

create policy "ritual_checkins_pair_access" on public.ritual_checkins for all to authenticated
using (user_id=(select auth.uid()) or exists(
 select 1 from public.rituals r join public.pair_members pm on pm.pair_id=r.pair_id
 where r.id=ritual_checkins.ritual_id::text and pm.user_id=(select auth.uid())
))
with check (user_id=(select auth.uid()) and exists(
 select 1 from public.rituals r join public.pair_members pm on pm.pair_id=r.pair_id
 where r.id=ritual_checkins.ritual_id::text and pm.user_id=(select auth.uid())
));

grant select, insert, update, delete on public.shared_tasks, public.partner_nudges, public.rituals to authenticated;
