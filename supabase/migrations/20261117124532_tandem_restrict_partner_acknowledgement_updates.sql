drop policy if exists nudges_update_member on public.partner_nudges;
create policy nudges_update_receiver on public.partner_nudges
for update to authenticated
using (receiver_user_id = (select auth.uid()))
with check (receiver_user_id = (select auth.uid()));

revoke update on public.partner_nudges from authenticated;
grant update (is_acknowledged, reply_chip, acknowledged_at) on public.partner_nudges to authenticated;

revoke update on public.notification_events from authenticated;
grant update (read_at) on public.notification_events to authenticated;
