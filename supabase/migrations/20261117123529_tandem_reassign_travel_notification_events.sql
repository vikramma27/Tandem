
create or replace function private.queue_travel_plan_notifications()
returns trigger language plpgsql security definer set search_path=public
as $$
begin
  if tg_op='UPDATE' then
    delete from public.notification_events
    where payload->>'plan_id'=new.id::text
      and delivered_at is null
      and event_type in ('travel_reminder','travel_departure');
  end if;
  if new.recipient_user_id is not null and new.recipient_user_id<>new.created_by then
    if tg_op='INSERT' or old.recipient_user_id is distinct from new.recipient_user_id then
      insert into public.notification_events(pair_id,recipient_user_id,actor_user_id,event_type,title,body,payload,scheduled_for)
      values(new.pair_id,new.recipient_user_id,new.created_by,'travel_plan_shared','A travel plan was shared',
        new.title||' is on the way to your shared plans',
        jsonb_build_object('plan_id',new.id,'destination',new.destination),now());
    end if;
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
revoke execute on function private.queue_travel_plan_notifications() from public,anon,authenticated;
