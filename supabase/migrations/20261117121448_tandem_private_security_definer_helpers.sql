
create schema if not exists private;
revoke all on schema private from public, anon;
grant usage on schema private to authenticated;

create or replace function private.is_pair_member(target_pair_id uuid, target_user_id uuid default auth.uid())
returns boolean language sql stable security definer set search_path=public
as $$ select exists(select 1 from public.pair_members where pair_id=target_pair_id and user_id=target_user_id); $$;
grant execute on function private.is_pair_member(uuid,uuid) to authenticated;

create or replace function private.create_pair_impl(pair_name text default 'Our space')
returns uuid language plpgsql security definer set search_path=public
as $$
declare new_pair_id uuid;
begin
  if auth.uid() is null then raise exception 'Authentication required'; end if;
  if exists(select 1 from public.pair_members where user_id=auth.uid()) then
    raise exception 'This account is already paired';
  end if;
  insert into public.couples(created_by,name) values(auth.uid(),coalesce(nullif(trim(pair_name),''),'Our space'))
  returning id into new_pair_id;
  insert into public.pair_members(pair_id,user_id,role) values(new_pair_id,auth.uid(),'owner');
  return new_pair_id;
end;
$$;
grant execute on function private.create_pair_impl(text) to authenticated;

create or replace function private.create_pair_invite_impl(pair_id_input uuid)
returns table(invite_code text, expires_at timestamptz)
language plpgsql security definer set search_path=public
as $$
declare raw_code text;
declare expiry timestamptz;
begin
  if auth.uid() is null or not exists(select 1 from public.pair_members where pair_id=pair_id_input and user_id=auth.uid()) then
    raise exception 'Not a member of this pair';
  end if;
  if (select count(*) from public.pair_members where pair_id=pair_id_input) >= 2 then
    raise exception 'This pair already has two members';
  end if;
  update public.pair_invites set expires_at=now()
    where pair_id=pair_id_input and accepted_at is null and expires_at>now();
  raw_code := upper(encode(gen_random_bytes(8),'hex'));
  expiry := now() + interval '48 hours';
  insert into public.pair_invites(pair_id,created_by,code_hash,expires_at)
    values(pair_id_input,auth.uid(),encode(digest(raw_code,'sha256'),'hex'),expiry);
  return query select raw_code,expiry;
end;
$$;
grant execute on function private.create_pair_invite_impl(uuid) to authenticated;

create or replace function private.accept_pair_invite_impl(invite_code_input text)
returns uuid language plpgsql security definer set search_path=public
as $$
declare target_invite public.pair_invites%rowtype;
declare members_count integer;
begin
  if auth.uid() is null then raise exception 'Authentication required'; end if;
  if exists(select 1 from public.pair_members where user_id=auth.uid()) then
    raise exception 'This account is already paired';
  end if;
  select * into target_invite from public.pair_invites
    where code_hash=encode(digest(upper(regexp_replace(trim(invite_code_input),'[^A-Z0-9]','','g')),'sha256'),'hex')
      and accepted_at is null and expires_at>now()
    for update;
  if not found then raise exception 'Invite is invalid or expired'; end if;
  select count(*) into members_count from public.pair_members where pair_id=target_invite.pair_id;
  if members_count>=2 then raise exception 'This pair already has two members'; end if;
  insert into public.pair_members(pair_id,user_id,role) values(target_invite.pair_id,auth.uid(),'member');
  update public.pair_invites set accepted_by=auth.uid(),accepted_at=now() where id=target_invite.id;
  return target_invite.pair_id;
end;
$$;
grant execute on function private.accept_pair_invite_impl(text) to authenticated;

create or replace function public.create_pair(pair_name text default 'Our space')
returns uuid language sql security invoker set search_path=public,private
as $$ select private.create_pair_impl(pair_name); $$;
create or replace function public.create_pair_invite(pair_id_input uuid)
returns table(invite_code text, expires_at timestamptz)
language sql security invoker set search_path=public,private
as $$ select * from private.create_pair_invite_impl(pair_id_input); $$;
create or replace function public.accept_pair_invite(invite_code_input text)
returns uuid language sql security invoker set search_path=public,private
as $$ select private.accept_pair_invite_impl(invite_code_input); $$;

revoke execute on function public.create_pair(text) from public,anon;
revoke execute on function public.create_pair_invite(uuid) from public,anon;
revoke execute on function public.accept_pair_invite(text) from public,anon;
grant execute on function public.create_pair(text) to authenticated;
grant execute on function public.create_pair_invite(uuid) to authenticated;
grant execute on function public.accept_pair_invite(text) to authenticated;

drop policy if exists pair_members_select_pair on public.pair_members;
create policy pair_members_select_pair on public.pair_members for select to authenticated
using (private.is_pair_member(pair_id,(select auth.uid())));

drop function if exists public.is_pair_member(uuid,uuid);
revoke execute on function private.is_pair_member(uuid,uuid) from public,anon;

create or replace function private.handle_new_user()
returns trigger language plpgsql security definer set search_path=public
as $$
begin
  insert into public.profiles(id,display_name)
  values(new.id,coalesce(new.raw_user_meta_data->>'display_name',''))
  on conflict(id) do nothing;
  return new;
end;
$$;
drop trigger if exists on_auth_user_created_tandem on auth.users;
create trigger on_auth_user_created_tandem after insert on auth.users
for each row execute procedure private.handle_new_user();
drop function if exists public.handle_new_user();
