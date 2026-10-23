#!/usr/bin/env python3
"""One-off (launch fixes, phase 6, item 18): adds kid Level 2 unit "Playing Flag", the rules
where NFL FLAG differs from tackle (ADR-015: the kid course follows NFL FLAG and says "your
league may differ"). Run once from the repo root.

Graded steps carry the existing `flag-football` concept, so no glossary term moves level and
the adult ladder needs nothing new. Source: NFL FLAG field rules (5 on 5).
"""
import json
from pathlib import Path

C = Path("shared/core/src/commonMain/resources/content")
FLAG = "flag-football"

UNIT = {
    "id": "kid2-u5",
    "title": "Playing Flag",
    "lessons": [
        {
            "id": "k2-flag-rules", "title": "How Flag Works",
            "blurb": "Flags, no blocking, and three plays to get there.",
            "steps": [
                {"type": "learn", "id": "k2-flag-01", "title": "Five on five",
                 "body": "In NFL FLAG, most games are five players against five. Every player wears a belt with flags. Pulling the ball carrier's flag ends the play, like a tackle. Your league may change a few rules, so ask your coach.",
                 "concept": FLAG},
                {"type": "learn", "id": "k2-flag-02", "title": "No blocking, no fumbles",
                 "body": "Nobody blocks or tackles in flag. The ball carrier can't push hands away or cover their flags; that's a penalty called flag guarding. If the ball hits the ground, the play is over. There are no fumbles.",
                 "concept": FLAG},
                {"type": "multiple_choice", "id": "k2-flag-03",
                 "prompt": "In NFL FLAG, the runner pushes a defender's hand away from their flag. What is that?",
                 "choices": ["Flag guarding, a penalty", "A legal block", "A fumble", "A stiff-arm, which is allowed"],
                 "answer": 0,
                 "explanation": "Runners can't protect their flags with a hand, an arm or the ball. That's flag guarding.",
                 "concept": FLAG},
                {"type": "true_false", "id": "k2-flag-04",
                 "statement": "In NFL FLAG, if the runner drops the ball, the defense can pick it up and run.",
                 "answer": False,
                 "explanation": "There are no fumbles in NFL FLAG. The play ends, and the ball goes where the runner's feet were.",
                 "concept": FLAG},
                {"type": "learn", "id": "k2-flag-05", "title": "Three plays at a time",
                 "body": "There are no kickoffs or punts. A team starts on its own 5-yard line and gets 3 plays to cross midfield, then 3 more to score. Just before midfield and just before the goal line are no-run zones. From there, you have to pass.",
                 "concept": FLAG},
                {"type": "multiple_choice", "id": "k2-flag-06",
                 "prompt": "Your team gets the ball in NFL FLAG. How many plays do you get to cross midfield?",
                 "choices": ["3", "4, like the NFL", "10", "As many as it takes"],
                 "answer": 0,
                 "explanation": "Three plays to cross midfield, then three more to score.",
                 "concept": FLAG},
                {"type": "multiple_choice", "id": "k2-flag-07",
                 "prompt": "The ball is in the no-run zone near the goal line. What does the offense have to do?",
                 "choices": ["Throw a pass", "Run the ball", "Kick a field goal", "Hand it to the center"],
                 "answer": 0,
                 "explanation": "In a no-run zone, the offense has to pass to score or cross midfield.",
                 "concept": FLAG},
            ],
        },
        {
            "id": "k2-flag-plays", "title": "Rushing and Scoring",
            "blurb": "The 7-yard rush, who can catch, and points after.",
            "steps": [
                {"type": "learn", "id": "k2-flag-08", "title": "Rushing the passer",
                 "body": "Defenders who rush the quarterback start 7 yards back from the line, and nobody may touch the quarterback. The quarterback can't run past the line but can hand off behind it. Every player on offense can catch a pass, even the center.",
                 "concept": FLAG},
                {"type": "learn", "id": "k2-flag-09", "title": "Points after a touchdown",
                 "body": "A touchdown is 6 points, and there's no kick after it. The offense gets one play from the 5-yard line for 1 point, or from the 10 for 2. The 5 is in the no-run zone, so the 1-point try has to be a pass.",
                 "concept": FLAG},
                {"type": "multiple_choice", "id": "k2-flag-10",
                 "prompt": "In NFL FLAG, where does a defender start to rush the quarterback?",
                 "choices": ["7 yards back from the line", "Right on the line", "Behind the quarterback", "In the end zone"],
                 "answer": 0,
                 "explanation": "Rushers start 7 yards back, so the quarterback has time to throw.",
                 "concept": FLAG},
                {"type": "true_false", "id": "k2-flag-11",
                 "statement": "In NFL FLAG, a try from the 10-yard line after a touchdown is worth 2 points.",
                 "answer": True,
                 "explanation": "From the 5 it's 1 point; from the farther 10 it's 2.",
                 "concept": FLAG},
                {"type": "multiple_choice", "id": "k2-flag-12",
                 "prompt": "In NFL FLAG, who can catch a pass?",
                 "choices": ["Every player on offense", "Only the wide receivers", "Only the receivers and the running back", "Everyone except the center"],
                 "answer": 0,
                 "explanation": "Every offensive player can catch a pass in NFL FLAG, even the center. The quarterback can too, after handing the ball off.",
                 "concept": FLAG},
                {"type": "multiple_choice", "id": "k2-flag-13",
                 "prompt": "In NFL FLAG, can the quarterback run past the line with the ball?",
                 "choices": ["No, but they can hand it off behind the line", "Yes, any time", "Only on the last play", "Only in a no-run zone"],
                 "answer": 0,
                 "explanation": "The quarterback can't run past the line, but a handoff behind it is fine.",
                 "concept": FLAG},
            ],
        },
    ],
}

path = C / "kid2/u5-playing-flag.json"
assert not path.exists()
path.write_text(json.dumps(UNIT, indent=2, ensure_ascii=False) + "\n")
cur = json.loads((C / "curriculum.json").read_text())
kid2 = next(l for l in cur["levels"] if l["id"] == "kid-2")
kid2["unitFiles"].append("kid2/u5-playing-flag.json")
(C / "curriculum.json").write_text(json.dumps(cur, indent=2, ensure_ascii=False) + "\n")
print("added kid2-u5 Playing Flag (13 steps)")
