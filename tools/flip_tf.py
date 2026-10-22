#!/usr/bin/env python3
"""One-off (launch fixes, phase 6): rewrite true/false items as false statements so
"always True" no longer scores 80%. Run from the repo root; edits content JSON in place."""
import json
from pathlib import Path

C = Path("shared/core/src/commonMain/resources/content")
FLIPS = {
    "k1-scoring-08": ("A field goal is worth 6 points.", "A field goal is worth 3. A touchdown is the one worth 6."),
    "k1-downs-06": ("When a team gets a first down, it gets just one more try.", "A first down gives 4 new tries to go 10 more yards."),
    "a1-basics-10": ("The defense is the team with the ball.", "The offense has the ball; the defense tries to stop it and take it away."),
    "k2-clock-04": ("The clock keeps running after an incomplete pass.", "A pass that is not caught stops the clock."),
    "a2-eligibility-08": ("A team may throw two forward passes on one play if both are thrown from behind the line of scrimmage.",
                          "Only one forward pass is allowed per play, and only from behind the line. Backward passes (laterals) are allowed anywhere, any number of times."),
    "a2-pi-06": ("In the NFL, a cornerback may jam a receiver 10 yards downfield before the pass is thrown.",
                 "NFL defenders may contact a receiver only within 5 yards of the line. Past that, before the throw, it's illegal contact: 5 yards and an automatic first down."),
    "a2-calls-06": ("Spiking the ball stops the clock without using a down.", "A spike stops the clock, but it counts as an incomplete pass, so it uses a down."),
    "a2-motion-07": ("After a shift, the offense may snap the ball right away.", "After a shift, everyone must be set for one second before the snap. Snapping sooner is an illegal shift."),
    "k3-moreroutes-07": ("A post route goes toward the sideline.", "A post goes deep toward the middle, aiming at the goal posts. The corner route is the one that breaks toward the sideline."),
    "k3-man-04": ("In man coverage, a defender guards one area and lets receivers pass through it.", "That is zone coverage. In man coverage, a defender follows his receiver wherever he goes."),
    "k3-smarts-05": ("In a two-minute drill, a runner should stay in bounds to stop the clock.", "Staying in bounds keeps the clock running. Going out of bounds stops it and saves precious seconds."),
    "k3-calls-04": ("If the play clock reaches zero before the snap, the defense gets a penalty.", "The offense gets the penalty: delay of game, 5 yards."),
    "a3-leverage-04": ("A defender with inside leverage is protecting the sideline.", "Inside leverage protects the middle and takes away inside-breaking routes. Outside leverage protects the sideline."),
    "a3-timing-05": ("Deep routes like the post usually need less time than the quick game.", "Deeper routes take longer to develop, so they pair with longer drops or play-action. Quick-game routes come out fast."),
    "v4-personnel-07": ("In 20 personnel there are two tight ends and no running backs.", "The first digit is running backs, the second tight ends: 20 is 2 backs, 0 tight ends, so 3 receivers."),
    "v4-gaps-08": ("A 5-technique lines up head-up on the center.", "Head-up on the center is a 0-technique. The 5 is the tackle's outside shoulder, in the C gap."),
    "v4-pressure-05": ("In a stunt, two defensive linemen drop into pass coverage.", "Dropping linemen into coverage is a zone blitz idea. In a stunt, or twist, two linemen cross paths to confuse the blockers."),
    "c5-option-08": ("Option plays try to block every defender.", "Option plays leave one defender unblocked on purpose. Reading him lets the offense block everyone else and still outnumber him."),
    "c5-pressure-08": ("A creeper sends six rushers after the quarterback.", "A creeper rushes only four, but from unexpected places: blitz-like confusion without leaving extra holes in coverage."),
    "c5-playaction-05": ("On a bootleg, the quarterback rolls out the same way as the run fake.", "On a bootleg the quarterback fakes the run one way and rolls out the other."),
    "p6-packages-06": ("A goal-line defense usually has fewer defensive linemen than a base defense.", "Goal-line defenses add linemen, five or six, to stop short-yardage runs."),
    "p6-presnap-07": ("Two high safeties usually mean more defenders near the line.", "Two deep leaves one fewer defender for the box, which is why runs look better against two-high shells."),
    "co7-offense-07": ("Stack and bunch formations make man coverage easier to play.", "Receivers close together create rubs and traffic, which makes man coverage harder."),
    "ar8-management-06": ("Releasing a player removes all of his money from the salary cap.", "Guaranteed money already paid or owed stays on the cap as dead money."),
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
                if s["id"] in FLIPS:
                    assert s["type"] == "true_false" and s["answer"] is True, s["id"]
                    s["statement"], s["explanation"] = FLIPS[s["id"]]
                    s["answer"] = False
                    done.add(s["id"]); changed = True
        if changed:
            path.write_text(json.dumps(unit, indent=2, ensure_ascii=False) + "\n")
missing = set(FLIPS) - done
assert not missing, missing
print(f"flipped {len(done)}")
