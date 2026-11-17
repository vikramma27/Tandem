# Tandem Supabase backend

The production project for Tandem is in the Mumbai region:

- Project ref: `iuzvjnxgcrmifecumyoo`
- API URL: `https://iuzvjnxgcrmifecumyoo.supabase.co`
- Region: `ap-south-1` (Mumbai)
- Database: PostgreSQL 17

## Schema

Apply `migrations/20261117120000_tandem_initial_schema.sql` to a fresh project. The schema includes:

- Supabase Auth profiles and one-to-one pair membership
- hashed, single-use pairing invitations with a 48-hour expiry
- shared travel plans and checklist items
- shared tasks, partner nudges, rituals and check-ins
- per-user device-token records and notification event queue
- row-level security policies and Realtime publication for shared data

The pairing RPCs are public-schema invoker functions that call restricted helpers in the non-exposed `private` schema. Do not expose the `private` schema through the Data API.

## Android Auth callback

Tandem uses the email magic-link flow and registers this Android callback:

`tandem://auth-callback`

Add that exact URI under **Authentication → URL Configuration → Redirect URLs** in the Supabase dashboard. The project must also have the Email provider enabled. Supabase's built-in mail service has low sending limits; configure a custom SMTP provider before inviting a partner whose address is not an allowed project-team address.

The Android client uses only the publishable key. Never put the `service_role` key in the APK, Gradle files, or source control.

## Travel sync and notifications

- Realtime updates shared travel plans while the app is connected.
- A unique 15-minute WorkManager job catches up after the partner device was offline.
- The receiving phone posts a native notification when a new plan arrives and schedules local reminder/departure alarms for the selected traveller.
- Exact alarms are used only when Android grants the permission; otherwise Android's inexact alarm fallback is used.

For prompt push delivery when the recipient app is closed or Android delays background work, configure Firebase Cloud Messaging. This needs a Firebase Android app configuration and a server-side Firebase service-account secret. Those credentials are intentionally not fabricated or committed here. The current WorkManager fallback is not an instant-push guarantee.

## Security

- RLS is enabled on all public application tables.
- Pair membership is checked inside policies using a restricted security-definer helper.
- Invite codes are stored as SHA-256 hashes, expire after 48 hours and are single-use.
- The Android app contains no service-role credential.
