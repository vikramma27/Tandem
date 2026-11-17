
drop policy if exists travel_plans_insert_member on public.travel_plans;
create policy travel_plans_insert_member on public.travel_plans for insert to authenticated
with check (
  created_by=(select auth.uid())
  and private.is_pair_member(pair_id,(select auth.uid()))
  and (recipient_user_id is null or private.is_pair_member(pair_id,recipient_user_id))
);
drop policy if exists travel_plans_update_member on public.travel_plans;
create policy travel_plans_update_member on public.travel_plans for update to authenticated
using (private.is_pair_member(pair_id,(select auth.uid())))
with check (
  private.is_pair_member(pair_id,(select auth.uid()))
  and (recipient_user_id is null or private.is_pair_member(pair_id,recipient_user_id))
);
drop policy if exists nudges_pair_access on public.partner_nudges;
create policy nudges_pair_access on public.partner_nudges for all to authenticated
using (private.is_pair_member(pair_id,(select auth.uid())))
with check (
  sender_user_id=(select auth.uid())
  and private.is_pair_member(pair_id,(select auth.uid()))
  and private.is_pair_member(pair_id,receiver_user_id)
);
