
drop policy if exists ritual_checkins_pair_access on public.ritual_checkins;
alter table public.ritual_checkins drop constraint if exists ritual_checkins_ritual_id_fkey;
alter table public.ritual_checkins alter column ritual_id type text using ritual_id::text;
alter table public.ritual_checkins add constraint ritual_checkins_ritual_id_fkey
 foreign key (ritual_id) references public.rituals(id) on delete cascade;
create policy "ritual_checkins_pair_access" on public.ritual_checkins for all to authenticated
using (user_id=(select auth.uid()) or exists(
 select 1 from public.rituals r join public.pair_members pm on pm.pair_id=r.pair_id
 where r.id=ritual_checkins.ritual_id and pm.user_id=(select auth.uid())
))
with check (user_id=(select auth.uid()) and exists(
 select 1 from public.rituals r join public.pair_members pm on pm.pair_id=r.pair_id
 where r.id=ritual_checkins.ritual_id and pm.user_id=(select auth.uid())
));
