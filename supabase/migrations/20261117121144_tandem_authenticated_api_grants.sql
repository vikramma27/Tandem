
grant usage on schema public to authenticated;
grant select, update on public.profiles to authenticated;
grant select on public.couples, public.pair_members, public.pair_invites to authenticated;
grant select, insert, update, delete on public.travel_plans, public.travel_checklist_items,
 public.shared_tasks, public.partner_nudges, public.rituals, public.ritual_checkins,
 public.device_tokens, public.notification_events to authenticated;
