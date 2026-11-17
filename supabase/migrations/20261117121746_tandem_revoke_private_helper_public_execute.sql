
revoke execute on function private.create_pair_impl(text) from public,anon;
revoke execute on function private.create_pair_invite_impl(uuid) from public,anon;
revoke execute on function private.accept_pair_invite_impl(text) from public,anon;
revoke execute on function private.handle_new_user() from public,anon,authenticated;
