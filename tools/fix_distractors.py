#!/usr/bin/env python3
"""One-off (launch fixes, phase 6): replace throwaway wrong answers ("Kneel", "Punt")
with plausible ones of similar length on adult and shared levels, so the right answer
no longer stands out as the only long, serious choice. Answers keep their index."""
import json
from pathlib import Path

C = Path("shared/core/src/commonMain/resources/content")
NEW = {
    "a1-basics-11": ["The ball carrier crosses the line of scrimmage", "The runner gains 10 yards for a first down", "A defender touches the runner with one hand"],
    "a1-clock-06": ["A timeout each team may call once per half", "A penalty for wasting time before halftime", "The last two minutes, when the clock never stops"],
    "a1-clock-11": ["A fumble: the defense can recover it", "A catch, as long as he holds on after", "A live ball until someone falls on it"],
    "a2-pi-09": ["A whistle and a flag at the spot", "Only on the stadium scoreboard", "A note to the head coach"],
    "a2-replay-06": ["The coach throws the red flag to challenge", "Nothing: catches can't be reviewed now", "Both coaches must agree to a review"],
    "a2-special-08": ["A timeout before the kick comes down", "A touchback: he'll down it in the end zone", "A return: blockers should set up a wall"],
    "v4-personnel-08": ["Add a linebacker to stop the run", "Stay in base with three linebackers", "Bring in a goal-line front"],
    "v4-man-05": ["A deep seven-step drop", "A long play-action fake", "Time to read the whole field"],
    "v4-man-06": ["Curl-flat: a curl and a flat route on one side", "Smash: a hitch under a corner route", "Flood: three routes at three depths on one side"],
    "v4-cover2-06": ["The safeties can't see the hitch", "The corners have to cover the deep middle", "Cover 2 has no flat defenders"],
    "v4-cover3-06": ["Cover 3 has no underneath defenders", "The corners must play man on the slots", "The single safety has to blitz"],
    "v4-cover4-06": ["The number of linemen in the front", "Linebacker depth: deeper in Cover 4", "Where the tight end lines up"],
    "c5-option-06": ["Hand it off to the running back", "Throw a quick pass over him", "Pitch it to the trailing receiver"],
    "c5-rpo-04": ["Hand off into the extra box defender", "Keep it and run up the middle", "Check to a deep shot downfield"],
    "c5-playaction-04": ["Linemen can block farther downfield", "The clock keeps running during the fake", "The quarterback gets a longer drop"],
    "p6-packages-04": ["Base 4-3 (four defensive backs)", "Goal line (three defensive backs)", "Base 3-4 (four defensive backs)"],
    "p6-tampa-05": ["The open area in the flats", "Run defense on the edges", "A four-man pass rush"],
    "p6-presnap-05": ["Run: more defenders leave more gaps open", "Run at the side with the extra defender", "Keep running until they back off"],
    "co7-late-05": ["So the offense can't hear the coverage call", "Because rotating early is a penalty", "To save the safeties' legs for the run"],
    "co7-pressure-06": ["Rushing six so the quarterback must throw hot", "Showing blitz, then rushing only three", "Sending a corner and playing man behind it"],
    "co7-coverage-07": ["One defender guarding two receivers", "A corner blitz from the slot side", "Four deep defenders splitting the field"],
    "co7-offense-05": ["Huddle longer so they have to show it", "Snap later in the play clock", "Keep extra blockers in on every play"],
    "co7-offense-06": ["Throw the flat route", "Check to a run", "Throw the out to the boundary"],
    "ar8-offense-08": ["Wide zone needs only two receivers, so the rest can block", "Bootlegs let linemen go downfield to block early", "The quarterback hands off and becomes a receiver"],
    "ar8-matchups-02": ["Outside, racing the ends to the edge", "Deep, because Wide-9 means two-high safeties", "Straight at the ends, to pin them wide"],
    "ar8-matchups-03": ["Inside runs between the tackles", "Quarterback sneaks", "Seven-step deep drops"],
    "ar8-analytics-06": ["Third-down plays always count as negative", "Six yards is below the league's average gain", "EPA only counts plays that score"],
    "ar8-analytics-07": ["It runs more than a typical team would in the same situations", "It passes on more than half of its plays", "It completes more passes than expected"],
    "ar8-fourth-04": ["Punts are blocked far more often than they used to be", "Defenses tire late in drives, so 4th downs are easier", "A failed try costs nothing in field position"],
    "ar8-management-04": ["Inside runs and quarterback sneaks", "Short passes to the flat", "Screens and draws"],
    "ar8-management-05": ["Rookie contracts don't count toward the cap", "Teams get an extra roster spot for a rookie", "Rookies are paid by the league, not the team"],
    "a3-pairs-04": ["Both receivers run the same route", "Two defenders cover one receiver", "The quarterback reads only the deep safety"],
}

cur = json.loads((C / "curriculum.json").read_text())
done = set()
for lv in cur["levels"]:
    for f in lv["unitFiles"]:
        path = C / f
        unit = json.loads(path.read_text())
        changed = False
        for lesson in unit["lessons"]:
            for s in lesson["steps"]:
                if s["id"] in NEW:
                    assert s["type"] == "multiple_choice" and len(s["choices"]) == 4, s["id"]
                    it = iter(NEW[s["id"]])
                    s["choices"] = [c if i == s["answer"] else next(it) for i, c in enumerate(s["choices"])]
                    done.add(s["id"]); changed = True
        if changed:
            path.write_text(json.dumps(unit, indent=2, ensure_ascii=False) + "\n")
assert done == set(NEW), set(NEW) - done
print(f"rewrote {len(done)}")
