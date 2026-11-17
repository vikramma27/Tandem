
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
  update public.pair_invites pi set expires_at=now()
    where pi.pair_id=pair_id_input and pi.accepted_at is null and pi.expires_at>now();
  raw_code := upper(encode(extensions.gen_random_bytes(8),'hex'));
  expiry := now() + interval '48 hours';
  insert into public.pair_invites(pair_id,created_by,code_hash,expires_at)
    values(pair_id_input,auth.uid(),encode(extensions.digest(raw_code::bytea,'sha256'),'hex'),expiry);
  return query select raw_code,expiry;
end;
$$;
revoke execute on function private.create_pair_invite_impl(uuid) from public,anon;
grant execute on function private.create_pair_invite_impl(uuid) to authenticated;
