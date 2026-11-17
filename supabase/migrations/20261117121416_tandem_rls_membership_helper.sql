
create or replace function public.is_pair_member(target_pair_id uuid, target_user_id uuid default auth.uid())
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists(
    select 1 from public.pair_members
    where pair_id = target_pair_id and user_id = target_user_id
  );
$$;
revoke execute on function public.is_pair_member(uuid,uuid) from public, anon;
grant execute on function public.is_pair_member(uuid,uuid) to authenticated;
drop policy if exists pair_members_select_pair on public.pair_members;
create policy pair_members_select_pair on public.pair_members for select to authenticated
using (public.is_pair_member(pair_id, (select auth.uid())));
