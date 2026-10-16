-- Gridiron Academy backend, first schema (SPEC 8.3) with row-level security (SPEC 8.5).
--
-- Who sees what (SPEC 5.4, 7.1):
--   * A parent sees and manages the children they created, and nothing about anyone else's.
--   * A coach (team staff) sees a learner only through an APPROVED team membership. A coach
--     can invite; only the parent (child) or the learner (own account) can approve.
--   * A child under 13 needs a parental consent record before any of their data is uploaded.
--   * Learner devices write events; nothing a learner writes is free text.
-- Every check lives in the database, so a bug in an app can't widen access.

-- ---------------------------------------------------------------------------------------
-- Types

create type public.age_band as enum ('4-5', '6-7', '8-9', '10-12', '13-17', '18+');
create type public.track as enum ('kid', 'adult');
create type public.member_status as enum ('pending', 'approved', 'revoked');
create type public.staff_role as enum ('owner', 'assistant');

-- ---------------------------------------------------------------------------------------
-- Tables

-- One row per signed-in adult (or teen with their own account). Created by the app after sign-up.
create table public.account (
    id uuid primary key references auth.users (id) on delete cascade,
    display_name text not null check (char_length(display_name) between 1 and 60),
    created_at timestamptz not null default now()
);

-- A learner: a nickname and an age band, never a real name, photo or birthday (SPEC 5.1).
create table public.learner (
    id uuid primary key default gen_random_uuid(),
    nickname text not null check (char_length(nickname) between 1 and 20),
    age_band public.age_band not null,
    track public.track not null,
    -- The parent who created this learner (a managed child) ...
    managed_by uuid references public.account (id) on delete cascade,
    -- ... or the learner's own account (13+).
    own_account uuid unique references public.account (id) on delete cascade,
    created_at timestamptz not null default now(),
    check (managed_by is not null or own_account is not null)
);

-- Verifiable parental consent (SPEC 5.4). The method and vendor are a legal decision; the
-- row records which was used and which version of the consent text the parent saw.
create table public.consent (
    learner_id uuid not null references public.learner (id) on delete cascade,
    parent_id uuid not null references public.account (id) on delete cascade,
    method text not null check (char_length(method) between 1 and 40),
    version text not null check (char_length(version) between 1 and 20),
    at timestamptz not null default now(),
    primary key (learner_id, version)
);

create table public.team (
    id uuid primary key default gen_random_uuid(),
    owner_id uuid not null references public.account (id) on delete cascade,
    name text not null check (char_length(name) between 1 and 60),
    variant text not null default '' check (char_length(variant) <= 40),
    age_group text not null default '' check (char_length(age_group) <= 40),
    season text not null default '' check (char_length(season) <= 40),
    created_at timestamptz not null default now()
);

create table public.staff (
    team_id uuid not null references public.team (id) on delete cascade,
    account_id uuid not null references public.account (id) on delete cascade,
    role public.staff_role not null,
    groups text[] not null default '{}',
    primary key (team_id, account_id)
);

create table public.team_member (
    team_id uuid not null references public.team (id) on delete cascade,
    learner_id uuid not null references public.learner (id) on delete cascade,
    status public.member_status not null default 'pending',
    positions text[] not null default '{}',
    groups text[] not null default '{}',
    created_at timestamptz not null default now(),
    primary key (team_id, learner_id)
);

-- The append-only event log (SPEC 8.1). Identity is (learner, device, seq): uploads are
-- idempotent, a repeat is a primary-key conflict the client ignores.
create table public.event (
    learner_id uuid not null references public.learner (id) on delete cascade,
    device_id text not null check (char_length(device_id) between 1 and 64),
    seq bigint not null check (seq >= 0),
    payload jsonb not null check (jsonb_typeof(payload) = 'object' and octet_length(payload::text) <= 4096),
    received_at timestamptz not null default now(),
    primary key (learner_id, device_id, seq)
);

-- Assignments are immutable once sent (SPEC 7.4): no update policy; an edit is a new row
-- with the next version.
create table public.assignment (
    id uuid primary key default gen_random_uuid(),
    version int not null default 1 check (version >= 1),
    team_id uuid references public.team (id) on delete cascade,
    created_by uuid not null references public.account (id) on delete cascade,
    from_name text not null check (char_length(from_name) between 1 and 60),
    pack_ids text[] not null check (cardinality(pack_ids) between 1 and 10),
    due_day bigint,
    pass_pct int not null default 80 check (pass_pct between 1 and 100),
    -- Notes reach children: no links, emails or phone numbers (SPEC 5.5). Same rules as
    -- NoteRules in the app; the app explains, the database enforces.
    note text not null default '' check (
        char_length(note) <= 200
        and note !~* '(https?://|www\.)'
        and note !~* '\m[a-z0-9-]+\.(com|net|org|io|co|app|gg|me|ly)\M'
        and note !~* '[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}'
        and note !~ '(\+?[0-9][ ().-]*){7,}'
    ),
    sent_day bigint not null,
    created_at timestamptz not null default now()
);

create table public.assignment_target (
    assignment_id uuid not null references public.assignment (id) on delete cascade,
    learner_id uuid not null references public.learner (id) on delete cascade,
    lessons_at_send int not null default 0 check (lessons_at_send >= 0),
    primary key (assignment_id, learner_id)
);

create index event_learner on public.event (learner_id);
create index team_member_learner on public.team_member (learner_id);
create index assignment_target_learner on public.assignment_target (learner_id);

-- ---------------------------------------------------------------------------------------
-- Access helpers. SECURITY DEFINER so policies can ask questions about rows the caller
-- can't read directly, without recursing into each other's policies. Each returns only
-- a boolean about the caller.

-- A learner device signs in with a token whose `learner_id` claim scopes it to one learner
-- (SPEC 8.5); the link-code exchange that mints it is an edge function.
create function public.device_learner() returns uuid
    language sql stable
    as $$ select nullif(auth.jwt() ->> 'learner_id', '')::uuid $$;

create function public.is_parent_of(l uuid) returns boolean
    language sql stable security definer set search_path = public
    as $$ select exists (select 1 from learner where id = l and managed_by = auth.uid()) $$;

create function public.is_self(l uuid) returns boolean
    language sql stable security definer set search_path = public
    as $$ select exists (select 1 from learner where id = l and own_account = auth.uid())
              or device_learner() = l $$;

create function public.is_staff(t uuid) returns boolean
    language sql stable security definer set search_path = public
    as $$ select exists (select 1 from staff where team_id = t and account_id = auth.uid()) $$;

create function public.is_owner(t uuid) returns boolean
    language sql stable security definer set search_path = public
    as $$ select exists (select 1 from team where id = t and owner_id = auth.uid()) $$;

-- Coaches see a learner only through an approved membership on a team they staff.
create function public.coaches(l uuid) returns boolean
    language sql stable security definer set search_path = public
    as $$ select exists (
        select 1 from team_member m join staff s on s.team_id = m.team_id
        where m.learner_id = l and m.status = 'approved' and s.account_id = auth.uid()) $$;

create function public.can_see_learner(l uuid) returns boolean
    language sql stable
    as $$ select public.is_parent_of(l) or public.is_self(l) or public.coaches(l) $$;

-- Under 13 (and anyone managed by a parent) needs consent on file before data leaves the
-- device (SPEC 5.4).
create function public.has_consent(l uuid) returns boolean
    language sql stable security definer set search_path = public
    as $$ select exists (select 1 from learner where id = l
                         and (age_band not in ('4-5', '6-7', '8-9', '10-12') or own_account is not null))
              or exists (select 1 from consent where learner_id = l) $$;

-- ---------------------------------------------------------------------------------------
-- Row-level security: on for every table, no access without a policy.

alter table public.account enable row level security;
alter table public.learner enable row level security;
alter table public.consent enable row level security;
alter table public.team enable row level security;
alter table public.staff enable row level security;
alter table public.team_member enable row level security;
alter table public.event enable row level security;
alter table public.assignment enable row level security;
alter table public.assignment_target enable row level security;

-- Signed-out callers get nothing.
revoke all on all tables in schema public from anon;

-- account: yourself only.
create policy account_self_read on public.account for select using (id = auth.uid());
create policy account_self_insert on public.account for insert with check (id = auth.uid());
create policy account_self_update on public.account for update using (id = auth.uid()) with check (id = auth.uid());

-- learner: parents create and manage their children; teens create themselves.
-- The row's own columns come first: an INSERT ... RETURNING must pass this policy, and a
-- helper that looks the learner up can't see the row being inserted yet.
create policy learner_read on public.learner for select
    using (managed_by = auth.uid() or own_account = auth.uid() or public.can_see_learner(id));
create policy learner_insert on public.learner for insert
    with check ((managed_by = auth.uid() and own_account is null) or (own_account = auth.uid() and managed_by is null));
create policy learner_update on public.learner for update
    using (managed_by = auth.uid() or own_account = auth.uid())
    with check (managed_by = auth.uid() or own_account = auth.uid());
-- Deleting a child deletes all of their cloud data (SPEC 5.4), cascading.
create policy learner_delete on public.learner for delete using (managed_by = auth.uid());

-- consent: only the managing parent records or reads it.
create policy consent_read on public.consent for select using (parent_id = auth.uid());
create policy consent_insert on public.consent for insert
    with check (parent_id = auth.uid() and public.is_parent_of(learner_id));

-- team: staff see it; parents and members see the teams they're on.
create policy team_read on public.team for select using (
    owner_id = auth.uid() or public.is_staff(id)
    or exists (select 1 from public.team_member m where m.team_id = id and (public.is_parent_of(m.learner_id) or public.is_self(m.learner_id))));
create policy team_insert on public.team for insert with check (owner_id = auth.uid());
create policy team_update on public.team for update using (owner_id = auth.uid()) with check (owner_id = auth.uid());
create policy team_delete on public.team for delete using (owner_id = auth.uid());

-- staff: staff see their team's staff; the owner adds and removes assistants.
create policy staff_read on public.staff for select using (public.is_staff(team_id) or public.is_owner(team_id));
create policy staff_insert on public.staff for insert with check (public.is_owner(team_id));
create policy staff_delete on public.staff for delete using (public.is_owner(team_id) and account_id <> auth.uid());

-- team_member: staff invite (always pending) and see their roster; the parent or the learner
-- sees it and approves through approve_membership(). Staff may edit positions and groups
-- only: column privileges below keep status out of their reach.
create policy member_read on public.team_member for select
    using (public.is_staff(team_id) or public.is_parent_of(learner_id) or public.is_self(learner_id));
create policy member_invite on public.team_member for insert
    with check (public.is_staff(team_id) and status = 'pending');
create policy member_edit on public.team_member for update
    using (public.is_staff(team_id)) with check (public.is_staff(team_id));
create policy member_leave on public.team_member for delete
    using (public.is_parent_of(learner_id) or public.is_self(learner_id) or public.is_owner(team_id));
revoke update on public.team_member from authenticated;
grant update (positions, groups) on public.team_member to authenticated;

-- event: append-only; written by the learner (device or own account) once consent is on
-- file; read by anyone who can see the learner.
create policy event_read on public.event for select using (public.can_see_learner(learner_id));
create policy event_insert on public.event for insert
    with check (public.is_self(learner_id) and public.has_consent(learner_id));

-- assignment: the sender and the targets' adults read it; staff of the team or a parent
-- send it. No update or delete: assignments are immutable.
create policy assignment_read on public.assignment for select using (
    created_by = auth.uid() or (team_id is not null and public.is_staff(team_id))
    or exists (select 1 from public.assignment_target t where t.assignment_id = id and public.can_see_learner(t.learner_id)));
create policy assignment_insert on public.assignment for insert
    with check (created_by = auth.uid() and (team_id is null or public.is_staff(team_id)));

-- assignment_target: only learners the sender can see, on an assignment the sender made.
create policy target_read on public.assignment_target for select using (public.can_see_learner(learner_id));
create policy target_insert on public.assignment_target for insert with check (
    public.can_see_learner(learner_id)
    and exists (select 1 from public.assignment a where a.id = assignment_id and a.created_by = auth.uid()));

-- ---------------------------------------------------------------------------------------
-- Functions the apps call.

-- Creating a team makes its owner the first staff member.
create function public.add_owner_as_staff() returns trigger
    language plpgsql security definer set search_path = public
    as $$ begin
        insert into staff (team_id, account_id, role) values (new.id, new.owner_id, 'owner');
        return new;
    end $$;
create trigger team_owner_is_staff after insert on public.team
    for each row execute function public.add_owner_as_staff();

-- The parent (or a learner with their own account) approves or revokes a team membership.
-- A child can't be approved without consent on file (SPEC 5.4: the coach invites, the
-- parent approves).
create function public.set_membership(t uuid, l uuid, approve boolean) returns void
    language plpgsql security definer set search_path = public
    as $$ begin
        if not (is_parent_of(l) or exists (select 1 from learner where id = l and own_account = auth.uid())) then
            raise exception 'only the parent or the learner can approve a membership';
        end if;
        if approve and not has_consent(l) then
            raise exception 'parental consent is required first';
        end if;
        update team_member set status = case when approve then 'approved' else 'revoked' end::member_status
            where team_id = t and learner_id = l;
        if not found then raise exception 'no such invitation'; end if;
    end $$;

revoke execute on function public.set_membership(uuid, uuid, boolean) from anon;
