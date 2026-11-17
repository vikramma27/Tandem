
create index if not exists couples_created_by_idx on public.couples(created_by);
create index if not exists pair_invites_pair_id_idx on public.pair_invites(pair_id);
create index if not exists pair_invites_created_by_idx on public.pair_invites(created_by);
create index if not exists pair_invites_accepted_by_idx on public.pair_invites(accepted_by);
create index if not exists travel_plans_created_by_idx on public.travel_plans(created_by);
create index if not exists travel_plans_recipient_idx on public.travel_plans(recipient_user_id);
create index if not exists travel_checklist_completed_by_idx on public.travel_checklist_items(completed_by);
create index if not exists shared_tasks_created_by_idx on public.shared_tasks(created_by);
create index if not exists shared_tasks_assigned_to_idx on public.shared_tasks(assigned_to);
create index if not exists shared_tasks_completed_by_idx on public.shared_tasks(completed_by);
create index if not exists partner_nudges_pair_idx on public.partner_nudges(pair_id);
create index if not exists partner_nudges_sender_idx on public.partner_nudges(sender_user_id);
create index if not exists partner_nudges_task_idx on public.partner_nudges(linked_task_id);
create index if not exists rituals_pair_idx on public.rituals(pair_id);
create index if not exists rituals_created_by_idx on public.rituals(created_by);
create index if not exists notification_events_pair_idx on public.notification_events(pair_id);
create index if not exists notification_events_actor_idx on public.notification_events(actor_user_id);
create index if not exists device_tokens_user_idx on public.device_tokens(user_id);

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
  update public.pair_invites set expires_at=now()
  where pair_id=pair_id_input and accepted_at is null and expires_at > now();
  raw_code := upper(encode(gen_random_bytes(8),'hex'));
  expiry := now() + interval '48 hours';
  insert into public.pair_invites(pair_id,created_by,code_hash,expires_at)
  values(pair_id_input,auth.uid(),encode(digest(raw_code,'sha256'),'hex'),expiry);
  return query select raw_code, expiry;
end;
$$;

revoke execute on function public.create_pair(text) from public, anon;
revoke execute on function public.create_pair_invite(uuid) from public, anon;
revoke execute on function public.accept_pair_invite(text) from public, anon;
revoke execute on function public.handle_new_user() from public, anon, authenticated;
grant execute on function public.create_pair(text) to authenticated;
grant execute on function public.create_pair_invite(uuid) to authenticated;
grant execute on function public.accept_pair_invite(text) to authenticated;
