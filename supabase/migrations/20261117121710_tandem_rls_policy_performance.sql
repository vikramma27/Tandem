
drop policy if exists profiles_select_self_or_partner on public.profiles;
create policy profiles_select_self_or_partner on public.profiles for select to authenticated
using (
 id=(select auth.uid()) or exists(
  select 1 from public.pair_members mine
  join public.pair_members theirs on theirs.pair_id=mine.pair_id
  where mine.user_id=(select auth.uid()) and theirs.user_id=profiles.id
 )
);
drop policy if exists profiles_update_self on public.profiles;
create policy profiles_update_self on public.profiles for update to authenticated
using(id=(select auth.uid())) with check(id=(select auth.uid()));

drop policy if exists couples_select_members on public.couples;
create policy couples_select_members on public.couples for select to authenticated
using(private.is_pair_member(id,(select auth.uid())));

drop policy if exists pair_invites_select_creator on public.pair_invites;
create policy pair_invites_select_creator on public.pair_invites for select to authenticated
using(created_by=(select auth.uid()));

drop policy if exists travel_plans_select_pair on public.travel_plans;
create policy travel_plans_select_pair on public.travel_plans for select to authenticated
using(private.is_pair_member(pair_id,(select auth.uid())));
drop policy if exists travel_plans_delete_member on public.travel_plans;
create policy travel_plans_delete_member on public.travel_plans for delete to authenticated
using(private.is_pair_member(pair_id,(select auth.uid())));

drop policy if exists travel_items_access_via_plan on public.travel_checklist_items;
create policy travel_items_access_via_plan on public.travel_checklist_items for all to authenticated
using(exists(
 select 1 from public.travel_plans tp
 where tp.id=travel_checklist_items.plan_id
 and private.is_pair_member(tp.pair_id,(select auth.uid()))
))
with check(exists(
 select 1 from public.travel_plans tp
 where tp.id=travel_checklist_items.plan_id
 and private.is_pair_member(tp.pair_id,(select auth.uid()))
));

drop policy if exists shared_tasks_pair_access on public.shared_tasks;
create policy shared_tasks_pair_access on public.shared_tasks for all to authenticated
using(private.is_pair_member(pair_id,(select auth.uid())))
with check(
 private.is_pair_member(pair_id,(select auth.uid()))
 and (created_by=(select auth.uid()) or private.is_pair_member(pair_id,created_by))
);

drop policy if exists rituals_pair_access on public.rituals;
create policy rituals_pair_access on public.rituals for all to authenticated
using(private.is_pair_member(pair_id,(select auth.uid())))
with check(private.is_pair_member(pair_id,(select auth.uid())));

drop policy if exists device_tokens_owner_access on public.device_tokens;
create policy device_tokens_owner_access on public.device_tokens for all to authenticated
using(user_id=(select auth.uid())) with check(user_id=(select auth.uid()));

drop policy if exists notifications_recipient_access on public.notification_events;
create policy notifications_recipient_access on public.notification_events for select to authenticated
using(recipient_user_id=(select auth.uid()));
drop policy if exists notifications_update_read on public.notification_events;
create policy notifications_update_read on public.notification_events for update to authenticated
using(recipient_user_id=(select auth.uid())) with check(recipient_user_id=(select auth.uid()));
