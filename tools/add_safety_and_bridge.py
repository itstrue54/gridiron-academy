#!/usr/bin/env python3
"""One-off (launch fixes, phase 6, item 10): adds the concussion card to both tracks and a
"Ready for Varsity" bridge unit at the end of kid Level 3. Run once from the repo root.

The bridge previews Level 4 words (personnel, gaps, cover numbers). Its questions carry no
concept tag on purpose: tagging them would make the glossary say those terms are taught at
Level 3, and an adult who finished adult Level 3 would then count as knowing them (ADR-010).
"""
import json
from pathlib import Path

C = Path("shared/core/src/commonMain/resources/content")


def p(pid, label, side, x, y):
    return {"id": pid, "label": label, "side": side, "x": x, "y": y}


LINE = [p("lt", "LT", "OFFENSE", -3.2, 0), p("lg", "LG", "OFFENSE", -1.6, 0), p("c", "C", "OFFENSE", 0, 0),
        p("rg", "RG", "OFFENSE", 1.6, 0), p("rt", "RT", "OFFENSE", 3.2, 0)]

# Gaps on the right side, in front of the line so a tap lands on the gap, not a lineman.
GAPS = {
    "players": LINE + [p("te", "TE", "OFFENSE", 4.8, 0), p("qb", "QB", "OFFENSE", 0, -5)],
    "zones": [
        {"id": "gap-a", "label": "A", "x": 0, "y": 1.5, "w": 1.6, "h": 3},
        {"id": "gap-b", "label": "B", "x": 1.6, "y": 1.5, "w": 1.6, "h": 3},
        {"id": "gap-c", "label": "C", "x": 3.2, "y": 1.5, "w": 1.6, "h": 3},
    ],
    "caption": "Gaps on the right side",
}

COVER2 = {
    "players": LINE + [
        p("qb", "QB", "OFFENSE", 0, -5), p("x", "X", "OFFENSE", -12, 0), p("z", "Z", "OFFENSE", 12, -1),
        p("de1", "DE", "DEFENSE", -4.5, 1), p("dt1", "DT", "DEFENSE", -1.2, 1), p("dt2", "DT", "DEFENSE", 1.2, 1),
        p("de2", "DE", "DEFENSE", 4.5, 1), p("w", "LB", "DEFENSE", -4, 4.5), p("m", "LB", "DEFENSE", 0, 4.5),
        p("s", "LB", "DEFENSE", 4, 4.5), p("cb1", "CB", "DEFENSE", -12, 5), p("cb2", "CB", "DEFENSE", 12, 5),
        p("fs1", "S", "DEFENSE", -9, 13), p("fs2", "S", "DEFENSE", 9, 13),
    ],
    "caption": "Two deep safeties, corners up",
}

BRIDGE = {
    "id": "kid3-u5",
    "title": "Ready for Varsity",
    "lessons": [
        {
            "id": "k3-ready-numbers", "title": "Counting Players",
            "blurb": "How coaches name the offense with two numbers.",
            "steps": [
                {"type": "learn", "id": "k3-ready-01", "title": "Two numbers",
                 "body": "Coaches name the offense with two numbers. The first counts running backs. The second counts tight ends. Everyone else who can catch a pass is a wide receiver."},
                {"type": "learn", "id": "k3-ready-02", "title": "11 personnel",
                 "body": "Besides the quarterback and the five linemen, five players can catch passes. \"11\" (say \"one-one\") means 1 running back and 1 tight end, so the other 3 are wide receivers."},
                {"type": "multiple_choice", "id": "k3-ready-03", "prompt": "In 12 personnel, how many tight ends are on the field?",
                 "choices": ["2", "1", "0", "3"], "answer": 0,
                 "explanation": "The second number counts tight ends: 12 is 1 running back and 2 tight ends."},
                {"type": "multiple_choice", "id": "k3-ready-04", "prompt": "In 11 personnel, how many wide receivers are there?",
                 "choices": ["3", "1", "2", "4"], "answer": 0,
                 "explanation": "1 running back and 1 tight end make 2. Five catchers minus 2 leaves 3 wide receivers."},
                {"type": "true_false", "id": "k3-ready-05", "statement": "In 21 personnel, there are 2 running backs.",
                 "answer": True, "explanation": "The first number counts running backs: 2 backs and 1 tight end."},
            ],
        },
        {
            "id": "k3-ready-gaps", "title": "Gaps Have Letters",
            "blurb": "The spaces between linemen, A, B and C.",
            "steps": [
                {"type": "learn", "id": "k3-ready-06", "title": "Gaps",
                 "body": "The spaces between the offensive linemen are called gaps. Runners run through them, and defenders try to fill them. Coaches name them with letters, starting in the middle."},
                {"type": "learn", "id": "k3-ready-07", "title": "A, B, C",
                 "body": "The A gap is next to the center. The B gap is between the guard and the tackle. The C gap is just outside the tackle. Each side of the line has all three.",
                 "diagram": GAPS},
                # A gap is 1.6 yards wide, too narrow to tap at phone width: name the shaded one.
                {"type": "multiple_choice", "id": "k3-ready-08", "prompt": "Which gap is shaded?",
                 "diagram": {**GAPS, "zones": [{"id": "gap-shaded", "label": "?", "x": 1.6, "y": 1.5, "w": 1.6, "h": 3}],
                             "caption": "One gap on the right side, shaded"},
                 "choices": ["The B gap", "The A gap", "The C gap", "The D gap"], "answer": 0,
                 "explanation": "It sits between the guard and the tackle, so it's the B gap."},
                {"type": "multiple_choice", "id": "k3-ready-09", "prompt": "Which gap is right next to the center?",
                 "choices": ["The A gap", "The B gap", "The C gap", "The D gap"], "answer": 0,
                 "explanation": "The letters start in the middle, so the A gap is beside the center."},
            ],
        },
        {
            "id": "k3-ready-covers", "title": "Coverage Numbers",
            "blurb": "Cover 1, 2 and 3: count the deep defenders.",
            "steps": [
                {"type": "learn", "id": "k3-ready-10", "title": "Count deep",
                 "body": "Many zone coverages are named by counting deep defenders. Cover 2 has 2 deep, each taking half of the deep field. Cover 3 has 3 deep, each taking a third."},
                {"type": "learn", "id": "k3-ready-11", "title": "Cover 1 and Cover 0",
                 "body": "Cover 1 is man coverage with 1 deep safety helping in the middle. Cover 0 is man coverage with nobody deep, so extra defenders can rush."},
                {"type": "multiple_choice", "id": "k3-ready-12", "prompt": "How many deep defenders does Cover 3 have?",
                 "choices": ["3", "1", "2", "4"], "answer": 0,
                 "explanation": "Cover 3 has three deep defenders, each taking a third of the deep field."},
                {"type": "true_false", "id": "k3-ready-13", "statement": "Cover 2 has one deep safety in the middle of the field.",
                 "answer": False,
                 "explanation": "Cover 2 has two deep safeties, each taking half. One deep safety in the middle is Cover 1 or Cover 3."},
                {"type": "diagram_tap", "id": "k3-ready-14", "prompt": "This defense is in Cover 2. Tap the two deep defenders.",
                 "diagram": COVER2, "targets": ["fs1", "fs2"],
                 "explanation": "The two safeties are deep, each guarding half of the field. That's where Cover 2 gets its name."},
            ],
        },
    ],
}

KID_SAFETY = [
    {"type": "learn", "id": "k2-safety-09", "title": "If your head hurts",
     "body": "After a bump or a hit, a headache, feeling dizzy, sick or sleepy, or seeing blurry can mean a concussion. Stop playing and tell your coach or a grown-up right away. A doctor decides when you can play again.",
     "concept": "concussion"},
    {"type": "true_false", "id": "k2-safety-10",
     "statement": "If you feel dizzy after a hit, you should keep playing so your team isn't short a player.",
     "answer": False,
     "explanation": "Stop and tell a grown-up. Playing on with a hurt head can make it much worse.",
     "concept": "concussion"},
    {"type": "multiple_choice", "id": "k2-safety-11",
     "prompt": "You bump heads in practice and now your head hurts. What should you do?",
     "choices": ["Stop and tell your coach or a grown-up", "Keep playing and tell nobody",
                 "Shake it off and run harder", "Wait until the game ends to say anything"],
     "answer": 0,
     "explanation": "Telling a grown-up right away is the brave and smart call. Your brain comes first.",
     "concept": "concussion"},
]

ADULT_SAFETY = [
    {"type": "learn", "id": "a2-protect-08", "title": "Concussions: sit out, then get cleared",
     "body": "Signs include headache, dizziness, confusion, nausea, blurred vision, or not remembering the play. Every US state has a youth sports concussion law, and most say a player suspected of one comes out and stays out until a health care provider clears them. Rules vary, so learn your state's and your league's. When in doubt, sit them out.",
     "concept": "concussion"},
    {"type": "multiple_choice", "id": "a2-protect-09",
     "prompt": "A youth player takes a hit, then seems confused about the play call. What should the coach do?",
     "choices": ["Take them out until a health care provider clears them",
                 "Let them sit one series, then put them back in",
                 "Ask if they feel OK and keep them in if they say yes",
                 "Have them drink some water and keep playing"],
     "answer": 0,
     "explanation": "Confusion after a hit is a concussion sign. They come out and stay out until a licensed provider clears them; a short rest or a player's \"I'm fine\" isn't enough.",
     "concept": "concussion"},
]

CONCUSSION = {
    "id": "concussion",
    "term": "Concussion",
    "kid": "A hurt brain from a hit to the head or body. If your head hurts or you feel dizzy after a hit, stop and tell a grown-up.",
    "full": "A brain injury caused by a blow to the head or body. A player with a suspected concussion should come out and stay out until a health care provider clears them; every US state has a youth sports concussion law, and most require this.",
    "level": 2,
    "side": "rules",
    "requires": [],
}


def add_steps(path, lesson_id, steps):
    unit = json.loads(path.read_text())
    lesson = next(l for l in unit["lessons"] if l["id"] == lesson_id)
    have = {s["id"] for s in lesson["steps"]}
    assert not have & {s["id"] for s in steps}, "already added"
    lesson["steps"] += steps
    path.write_text(json.dumps(unit, indent=2, ensure_ascii=False) + "\n")


add_steps(C / "kid2/u2-referees-and-rules.json", "k2-safety", KID_SAFETY)
add_steps(C / "adult2/u2-rules-that-decide-games.json", "a2-protect", ADULT_SAFETY)

g = json.loads((C / "glossary.json").read_text())
assert all(e["id"] != "concussion" for e in g["entries"])
g["entries"].append(CONCUSSION)
(C / "glossary.json").write_text(json.dumps(g, indent=2, ensure_ascii=False) + "\n")

bridge = C / "kid3/u5-ready-for-varsity.json"
assert not bridge.exists()
bridge.write_text(json.dumps(BRIDGE, indent=2, ensure_ascii=False) + "\n")
cur = json.loads((C / "curriculum.json").read_text())
kid3 = next(l for l in cur["levels"] if l["id"] == "kid-3")
kid3["unitFiles"].append("kid3/u5-ready-for-varsity.json")
(C / "curriculum.json").write_text(json.dumps(cur, indent=2, ensure_ascii=False) + "\n")
print("added: 3 kid + 2 adult safety steps, glossary 'concussion', kid3-u5 (14 steps)")
