-- Assignments remember whether warm-ups for missing basics are included (SPEC 7.4,
-- ADR-004). Every app computes each learner's plan from the packs, the concept graph and
-- mastery; this flag makes them all compute the same one.
alter table public.assignment add column include_warmups boolean not null default true;

drop function public.send_assignment(uuid, text, text[], bigint, int, text, bigint, jsonb);

create function public.send_assignment(
    team uuid,
    from_name text,
    pack_ids text[],
    due_day bigint,
    pass_pct int,
    note text,
    sent_day bigint,
    targets jsonb,
    include_warmups boolean default true
) returns uuid
    language plpgsql security invoker set search_path = public
    as $$
    declare a uuid;
    begin
        if jsonb_typeof(targets) <> 'array' or jsonb_array_length(targets) = 0 then
            raise exception 'an assignment needs at least one learner';
        end if;
        insert into assignment (team_id, created_by, from_name, pack_ids, due_day, pass_pct, note, sent_day, include_warmups)
            values (team, auth.uid(), from_name, pack_ids, due_day, pass_pct, note, sent_day, include_warmups)
            returning id into a;
        insert into assignment_target (assignment_id, learner_id, lessons_at_send)
            select a, (t ->> 'learner_id')::uuid, coalesce((t ->> 'lessons_at_send')::int, 0)
            from jsonb_array_elements(targets) t;
        return a;
    end $$;

revoke execute on function public.send_assignment(uuid, text, text[], bigint, int, text, bigint, jsonb, boolean) from anon;
