
create or replace function private.accept_pair_invite_impl(invite_code_input text)
returns uuid language plpgsql security definer set search_path=public
as $$
declare target_invite public.pair_invites%rowtype;
declare members_count integer;
declare normalized_code text;
begin
  if auth.uid() is null then raise exception 'Authentication required'; end if;
  if exists(select 1 from public.pair_members where user_id=auth.uid()) then
    raise exception 'This account is already paired';
  end if;
  normalized_code := regexp_replace(upper(trim(invite_code_input)),'[^A-Z0-9]','','g');
  select * into target_invite from public.pair_invites
    where code_hash=encode(extensions.digest(normalized_code::bytea,'sha256'),'hex')
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
revoke execute on function private.accept_pair_invite_impl(text) from public,anon;
grant execute on function private.accept_pair_invite_impl(text) to authenticated;
