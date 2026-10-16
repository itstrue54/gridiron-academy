# Gridiron Academy backend (Supabase)

Schema, row-level security and database functions for the Coach & Parent app
(SPEC 8, ADR-004). Everything that decides who sees what is in
`supabase/migrations/`, enforced by Postgres.

## Test (no Docker, no account needed)

```bash
cd backend
npm install
npm test
```

The tests run every migration on PGlite (real Postgres in Node) after a small stand-in
for Supabase's `auth` schema (`test/supabase-stub.sql`). Each test acts as one person,
a parent, a coach, a stranger or a child's device, and checks what the database lets
them see and do.

## Connect a Supabase project (one time)

1. Create a project at supabase.com. Pick the region where your users are; data stays
   there (SPEC 5.4).
2. Sign the CLI in and link this folder to the project (the project ref is in the
   project's URL, `https://<ref>.supabase.co`):

   ```bash
   cd backend
   npx supabase@2.119.0 login
   npx supabase@2.119.0 link --project-ref <ref>
   npx supabase@2.119.0 db push
   ```

3. In the dashboard, Project Settings > API: copy the **Project URL** and the **anon
   public** key into the repo's `local.properties` (never committed):

   ```
   supabase.url=https://<ref>.supabase.co
   supabase.anonKey=<anon public key>
   ```

   Never use the `service_role` key in an app. It bypasses row-level security.

4. Authentication > Providers > Email: leave "Confirm email" on. New coaches confirm by
   the emailed link, then sign in in the app.

5. Build the Coach app. With those two lines present it signs in to the project;
   without them it runs on the built-in demo account.

## What's here

| Migration | What it adds |
|---|---|
| `20261016000000_init.sql` | Tables, RLS on every table, `set_membership` (parent or teen approves a team invitation; children need consent first) |
| `20261016000100_send_assignment.sql` | `send_assignment`: an assignment and its players in one transaction |

Not yet: the edge function that links a child's device and mints its learner-scoped
token, the consent flow itself (VPC vendor is a legal decision), playbook and report tables.
