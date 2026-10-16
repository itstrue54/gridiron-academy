-- Sending an assignment writes the assignment and its targets in one transaction, so a
-- failure part-way (a target the sender can't see, a note the check refuses) leaves
-- nothing behind. SECURITY INVOKER: every row-level policy still applies to the caller.
create function public.send_assignment(
    team uuid,
    from_name text,
    pack_ids text[],
    due_day bigint,
    pass_pct int,
    note text,
    sent_day bigint,
    targets jsonb  -- [{"learner_id": "...", "lessons_at_send": 3}, ...]
) returns uuid
    language plpgsql security invoker set search_path = public
    as $$
    declare a uuid;
    begin
        if jsonb_typeof(targets) <> 'array' or jsonb_array_length(targets) = 0 then
            raise exception 'an assignment needs at least one learner';
        end if;
        insert into assignment (team_id, created_by, from_name, pack_ids, due_day, pass_pct, note, sent_day)
            values (team, auth.uid(), from_name, pack_ids, due_day, pass_pct, note, sent_day)
            returning id into a;
        insert into assignment_target (assignment_id, learner_id, lessons_at_send)
            select a, (t ->> 'learner_id')::uuid, coalesce((t ->> 'lessons_at_send')::int, 0)
            from jsonb_array_elements(targets) t;
        return a;
    end $$;

revoke execute on function public.send_assignment(uuid, text, text[], bigint, int, text, bigint, jsonb) from anon;
