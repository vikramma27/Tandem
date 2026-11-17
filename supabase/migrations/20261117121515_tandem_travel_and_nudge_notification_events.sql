
create or replace function private.queue_travel_plan_notifications()
returns trigger language plpgsql security definer set search_path=public
as $$
begin
  if tg_op='UPDATE' then
    delete from public.notification_events
    where payload->>'plan_id'=new.id::text
      and recipient_user_id=new.recipient_user_id
      and delivered_at is null
      and event_type in ('travel_reminder','travel_departure');
  end if;
  if new.recipient_user_id is not null and new.recipient_user_id <> new.created_by then
    insert into public.notification_events(pair_id,recipient_user_id,actor_user_id,event_type,title,body,payload,scheduled_for)
    values(new.pair_id,new.recipient_user_id,new.created_by,'travel_plan_shared','A travel plan was shared',
      new.title || ' is on the way to your shared plans',
      jsonb_build_object('plan_id',new.id,'destination',new.destination),now());
  end if;
  if new.recipient_user_id is not null and new.reminder_at is not null and new.reminder_at>now() then
    insert into public.notification_events(pair_id,recipient_user_id,actor_user_id,event_type,title,body,payload,scheduled_for)
    values(new.pair_id,new.recipient_user_id,new.created_by,'travel_reminder','Travel preparation reminder',
      'Time to check the essentials for ' || new.title,
      jsonb_build_object('plan_id',new.id),new.reminder_at);
  end if;
  if new.recipient_user_id is not null and new.departure_at>now() then
    insert into public.notification_events(pair_id,recipient_user_id,actor_user_id,event_type,title,body,payload,scheduled_for)
    values(new.pair_id,new.recipient_user_id,new.created_by,'travel_departure','Departure time',
      'It is time to leave for ' || new.title,
      jsonb_build_object('plan_id',new.id),new.departure_at);
  end if;
  return new;
end;
$$;

drop trigger if exists travel_plan_notifications on public.travel_plans;
create trigger travel_plan_notifications
after insert or update of title,destination,departure_at,reminder_at,recipient_user_id,status
on public.travel_plans for each row execute procedure private.queue_travel_plan_notifications();

create or replace function private.queue_nudge_notification()
returns trigger language plpgsql security definer set search_path=public
as $$
begin
  insert into public.notification_events(pair_id,recipient_user_id,actor_user_id,event_type,title,body,payload,scheduled_for)
  values(new.pair_id,new.receiver_user_id,new.sender_user_id,'partner_nudge','A little nudge from your partner',
    new.message,jsonb_build_object('nudge_id',new.id,'linked_task_id',new.linked_task_id),now());
  return new;
end;
$$;
drop trigger if exists partner_nudge_notification on public.partner_nudges;
create trigger partner_nudge_notification
after insert on public.partner_nudges for each row execute procedure private.queue_nudge_notification();
