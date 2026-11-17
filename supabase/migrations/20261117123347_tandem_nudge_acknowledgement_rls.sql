
drop policy if exists nudges_pair_access on public.partner_nudges;
drop policy if exists nudges_pair_select on public.partner_nudges;
drop policy if exists nudges_insert_sender on public.partner_nudges;
drop policy if exists nudges_update_member on public.partner_nudges;
drop policy if exists nudges_delete_sender on public.partner_nudges;

create policy nudges_pair_select on public.partner_nudges for select to authenticated
using(private.is_pair_member(pair_id,(select auth.uid())));
create policy nudges_insert_sender on public.partner_nudges for insert to authenticated
with check(
 sender_user_id=(select auth.uid())
 and private.is_pair_member(pair_id,(select auth.uid()))
 and private.is_pair_member(pair_id,receiver_user_id)
);
create policy nudges_update_member on public.partner_nudges for update to authenticated
using(private.is_pair_member(pair_id,(select auth.uid())))
with check(private.is_pair_member(pair_id,(select auth.uid())));
create policy nudges_delete_sender on public.partner_nudges for delete to authenticated
using(sender_user_id=(select auth.uid()));
