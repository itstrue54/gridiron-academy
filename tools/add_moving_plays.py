#!/usr/bin/env python3
"""One-off: gives the Level 1-2 teaching cards that describe a play a diagram that runs it
(film mode, DESIGN 7.8), ball and all. Levels 1-2 were nearly all text; the play moving
is what makes a card like "Handoff" land. Run once from the repo root.

Pictures are whiteboard drawings: only the players the card is about, so they read at
phone width. Offense moves up the screen; y is yards from the line of scrimmage.
"""
import json
import math
from pathlib import Path

C = Path("shared/core/src/commonMain/resources/content")


def p(pid, label, side, x, y):
    return {"id": pid, "label": label, "side": side, "x": x, "y": y}


def o(pid, label, x, y):
    return p(pid, label, "OFFENSE", x, y)


def d(pid, label, x, y):
    return p(pid, label, "DEFENSE", x, y)


def r(pid, kind, *pts):
    return {"playerId": pid, "points": [{"x": x, "y": y} for x, y in pts], "kind": kind}


def zone(zid, label, x, y, w, h):
    return {"id": zid, "label": label, "x": x, "y": y, "w": w, "h": h}


def ball(*spots):
    return [{"playerId": pid, "at": at} for pid, at in spots]


def share(start, pts, upto):
    """The fraction of a path's length covered once the player reaches pts[upto]."""
    legs = list(zip([start] + pts[:-1], pts))
    lens = [math.dist(a, b) for a, b in legs]
    return round(sum(lens[:upto + 1]) / sum(lens), 2)


INTERIOR = [o("lg", "LG", -1.6, 0), o("c", "C", 0, 0), o("rg", "RG", 1.6, 0)]
LINE = [o("lt", "LT", -3.2, 0)] + INTERIOR + [o("rt", "RT", 3.2, 0)]

TOUCHDOWN = {
    "players": INTERIOR + [o("qb", "QB", 0, -1), o("rb", "RB", 0, -5),
                           d("dl1", "DL", -1, 1), d("dl2", "DL", 1, 1), d("lb", "LB", 0, 5), d("s", "S", 4, 9)],
    "routes": [r("rg", "block", (1.2, 0.6)), r("rb", "run", (1.2, -1.5), (2.6, 0), (2.8, 8), (1.5, 15)),
               r("lb", "blitz", (1.5, 5.5), (3.4, 7)), r("s", "blitz", (3, 11), (2.8, 12.5))],
    "zones": [zone("ez", "End zone", -9, 12, 18, 10)],
    "ball": ball(("qb", 0), ("rb", 0.2)),
    "depth": 22, "behind": 7,
    "caption": "The runner carries it into the end zone",
}

FIRST_DOWN = {
    "players": INTERIOR + [o("qb", "QB", 0, -1), o("rb", "RB", 0, -5),
                           d("dl1", "DL", -1, 1), d("dl2", "DL", 1, 1), d("lb", "LB", -1, 5)],
    "routes": [r("lg", "block", (-1.2, 0.6)), r("rb", "run", (-1.2, -1.5), (-2.6, 0), (-2.9, 7), (-2.2, 12)),
               r("lb", "blitz", (-2, 6), (-3.6, 7.5))],
    "zones": [zone("fd", "First down", -9, 10, 18, 0.6)],
    "ball": ball(("qb", 0), ("rb", 0.2)),
    "depth": 14, "behind": 7,
    "caption": "Past the line 10 yards ahead: a first down",
}

SNAP = {
    "players": LINE + [o("qb", "QB", 0, -5)],
    "ball": ball(("c", 0), ("qb", 0.35)),
    "depth": 4, "behind": 7,
    "caption": "The center snaps the ball back to the quarterback",
}

HANDOFF = {
    "players": INTERIOR + [o("qb", "QB", 0, -1), o("rb", "RB", 0, -6), d("lb", "LB", -2, 5)],
    "routes": [r("rb", "run", (0.5, -2.2), (0.8, 0), (1.2, 9))],
    "ball": ball(("qb", 0), ("rb", 0.28)),
    "depth": 11, "behind": 8,
    "caption": "Handoff: the QB gives it to the running back",
}

PASS = {
    "players": INTERIOR + [o("qb", "QB", 0, -5), o("wr", "WR", -12, 0), d("cb", "CB", -12, 6)],
    "routes": [r("wr", "pass", (-12, 10), (-7, 15)), r("cb", "drop", (-11.5, 8), (-10, 10))],
    "ball": ball(("qb", 0), ("wr", 0.9)),
    "depth": 17, "behind": 7,
    "caption": "Pass: the QB throws to the receiver",
}

TACKLE = {
    "players": INTERIOR + [o("rb", "RB", 0, -4), d("lb", "LB", 5, 6)],
    "routes": [r("rb", "run", (2.4, -0.5), (2.6, 4)), r("lb", "blitz", (3.8, 5.2), (2.8, 4.6))],
    "ball": ball(("rb", 0)),
    "depth": 9, "behind": 6,
    "caption": "The linebacker meets the runner",
}

_CB_START = (-13, 6)
_CB_PATH = [(-7, 11.5), (-9, 8.5)]
INTERCEPTION = {
    "players": [o("c", "C", 0, 0), o("qb", "QB", 0, -5), o("wr", "WR", -12, 0), d("cb", "CB", *_CB_START)],
    "routes": [r("wr", "pass", (-12, 6), (-5, 12.5)), r("cb", "run", *_CB_PATH)],
    "ball": ball(("qb", 0), ("cb", share(_CB_START, _CB_PATH, 0))),
    "depth": 15, "behind": 7,
    "caption": "The cornerback jumps in front and catches it",
}

BLOCKS = {
    "players": LINE + [o("qb", "QB", 0, -1), o("rb", "RB", 0, -5),
                       d("de1", "DE", -4, 1), d("dt1", "DT", -1, 1), d("dt2", "DT", 1.2, 1), d("de2", "DE", 4, 1)],
    "routes": [r("lt", "block", (-3.8, 0.3)), r("lg", "block", (-1.2, 0.3)), r("c", "block", (-0.4, 0.3)),
               r("rg", "block", (1.1, 0.3)), r("rt", "block", (3.9, 0.3)),
               r("rb", "run", (1.4, -1.5), (2.4, 0), (2.6, 8))],
    "ball": ball(("qb", 0), ("rb", 0.22)),
    "depth": 10, "behind": 7,
    "caption": "Blockers push defenders aside and open a hole",
}

PUNT = {
    "players": [o("ls", "LS", 0, 0), o("g1", "G", -18, 0), o("g2", "G", 18, 0), o("p", "P", 0, -12), d("pr", "PR", 0, 28)],
    "routes": [r("g1", "pass", (-12, 16), (-3, 25)), r("g2", "pass", (12, 16), (3, 25))],
    "ball": ball(("ls", 0), ("p", 0.12), ("pr", 0.85)),
    "depth": 31, "behind": 14,
    "caption": "A punt: kicked far away, and the gunners race to the returner",
}

MOTION = {
    "players": LINE + [o("te", "TE", 4.8, 0), o("x", "X", -14, 0), o("qb", "QB", 0, -1), o("rb", "RB", 0, -6), o("z", "Z", 14, -1)],
    "routes": [r("z", "motion", (6, -1))],
    "ball": ball(("c", 0), ("qb", 0.95)),
    "depth": 6, "behind": 8,
    "caption": "Z runs across, parallel to the line, before the snap",
}

PLAYS = {
    "kid1": {
        "k1-scoring-01": TOUCHDOWN, "k1-downs-02": FIRST_DOWN, "k1-downs-03": PUNT, "k1-moving-01": SNAP,
        "k1-moving-02": HANDOFF, "k1-moving-03": PASS, "k1-moving-04": TACKLE, "k1-turnovers-02": INTERCEPTION,
        "k1-positions-03": BLOCKS,
    },
    "kid2": {"k2-runpass-01": HANDOFF, "k2-runpass-02": PASS, "k2-oline-02": BLOCKS},
    "adult1": {"a1-scoring-01": TOUCHDOWN, "a1-downs-01": FIRST_DOWN, "a1-turnovers-01": INTERCEPTION, "a1-turnovers-02": PUNT},
    "adult2": {"a2-motion-01": MOTION},
}

added = 0
for folder, plays in PLAYS.items():
    for f in sorted((C / folder).glob("*.json")):
        unit = json.loads(f.read_text())
        changed = False
        for lesson in unit["lessons"]:
            for s in lesson["steps"]:
                if s["id"] in plays:
                    assert s["type"] == "learn" and "diagram" not in s, s["id"]
                    # The diagram sits before the concept tag, as on other cards.
                    concept = s.pop("concept", None)
                    s["diagram"] = plays[s["id"]]
                    if concept:
                        s["concept"] = concept
                    changed = True
                    added += 1
        if changed:
            f.write_text(json.dumps(unit, indent=2, ensure_ascii=False) + "\n")
assert added == sum(len(v) for v in PLAYS.values()), added
print(f"{added} cards now run a play")
