# Tandem 1.1.0 release verification

This checklist is a release gate, not a statement that the checks have passed.

## Automated
- [ ] Android debug APK builds
- [ ] Android release APK builds
- [ ] APK artifacts are uploaded by GitHub Actions

## Auth and pairing (two separate Android devices)
- [ ] Email magic-link sign-in opens the app via `tandem://auth-callback`
- [ ] New account profile is created automatically
- [ ] First user creates a pair and a 48-hour invite
- [ ] Second user accepts the invite
- [ ] Expired, invalid, already-used, and third-member invites are rejected
- [ ] Pair members can see each other's display names, but unrelated accounts cannot

## Travel sync
- [ ] User A creates a plan for User B
- [ ] User B receives the shared-plan notification while Tandem is foregrounded
- [ ] User B sees the plan and checklist on the Travel Prep screen
- [ ] Checklist changes appear immediately on the partner while Travel Prep is open
- [ ] Checklist changes converge in both directions
- [ ] Plan deletion is reflected on the other device
- [ ] Create/update/delete while offline; reconnect and confirm the local outbox replays changes
- [ ] Device reconnects after being offline and catches up through WorkManager
- [ ] Editing a plan does not create duplicate or stale reminder events

## Android reminders
- [ ] Notification permission denied / granted paths behave correctly
- [ ] Exact-alarm permission granted path behaves correctly
- [ ] Inexact fallback path behaves correctly
- [ ] Reboot, app replacement, time and timezone changes reschedule alarms
- [ ] Alarm cancellation works after deleting a plan
- [ ] Cancel/delete a plan and confirm pending server notification events are removed
- [ ] Force-stop / vendor battery restrictions are documented as OS limitations

## Release
- [ ] Supabase redirect allowlist contains `tandem://auth-callback`
- [ ] Email provider and SMTP delivery tested with real addresses
- [ ] Security and performance advisors reviewed
- [ ] No service-role credential is present in Android sources or APK
- [ ] Manual two-device test evidence attached to the PR
- [ ] Only then merge and publish v1.1.0
