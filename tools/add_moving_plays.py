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


def push(pid, *pts):
    """A reaction, not a play: the player is driven along it, but no line is drawn."""
    return {**r(pid, "block", *pts), "drawn": False}


def ball(*spots):
    """(player, at) for a hand, or (x, y, at) for the ball loose or in the air."""
    return [{"playerId": s[0], "at": s[1]} if len(s) == 2 else {"x": s[0], "y": s[1], "at": s[2]} for s in spots]


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
    "routes": [r("rg", "block", (1.2, 0.6)), push("dl2", (0.4, 1.8)), r("rb", "run", (1.2, -1.5), (2.6, 0), (2.8, 8), (1.5, 15)),
               r("lb", "blitz", (1.5, 5.5), (3.4, 7)), r("s", "blitz", (3, 11), (2.8, 12.5))],
    "zones": [zone("ez", "End zone", -9, 12, 18, 10)],
    "ball": ball(("qb", 0), ("rb", 0.2)),
    "depth": 22, "behind": 7,
    "caption": "The runner carries it into the end zone",
}

FIRST_DOWN = {
    "players": INTERIOR + [o("qb", "QB", 0, -1), o("rb", "RB", 0, -5),
                           d("dl1", "DL", -1, 1), d("dl2", "DL", 1, 1), d("lb", "LB", -1, 5)],
    "routes": [r("lg", "block", (-1.2, 0.6)), push("dl1", (-0.4, 1.8)), r("rb", "run", (-1.2, -1.5), (-2.6, 0), (-2.9, 7), (-2.2, 12)),
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
               push("dt2", (0.6, 1.8)), push("de2", (4.9, 1.9)), push("dt1", (-1.1, 1.9)), push("de1", (-4.2, 1.9)),
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

PASS_PRO = {
    "players": LINE + [o("qb", "QB", 0, -1), o("wr", "WR", -12, 0),
                       d("de1", "DE", -4.6, 1), d("dt1", "DT", -1, 1), d("dt2", "DT", 1.2, 1), d("de2", "DE", 4.6, 1)],
    "routes": [r("lt", "block", (-4.2, -1.6)), r("lg", "block", (-1.4, -1.2)), r("c", "block", (0, -1.2)),
               r("rg", "block", (1.4, -1.2)), r("rt", "block", (4.2, -1.6)),
               r("de1", "blitz", (-5.4, -0.6)), r("dt1", "blitz", (-1.2, -0.4)),
               r("dt2", "blitz", (1.3, -0.4)), r("de2", "blitz", (5.4, -0.6)),
               push("qb", (0, -6)), r("wr", "pass", (-12, 12))],
    "ball": ball(("qb", 0), ("wr", 0.92)),
    "depth": 14, "behind": 8,
    "caption": "On a pass, the line makes a wall so the QB has time to throw",
}

# Backed up at the 2: the goal line is at y = -2, the end zone behind it.
SAFETY = {
    "players": INTERIOR + [o("qb", "QB", 0, -1), o("rb", "RB", 0, -5), d("de", "DE", -4, 1), d("dt", "DT", 1, 1)],
    "routes": [r("rb", "run", (-1.2, -3.6), (-4.2, -4.4)), r("de", "blitz", (-4.4, -1.5), (-4.4, -4))],
    "zones": [zone("ez", "End zone", -6, -9, 12, 7)],
    "ball": ball(("qb", 0), ("rb", 0.3)),
    "depth": 3, "behind": 9,
    "caption": "Tackled in their own end zone: a safety, 2 points",
}

# The hit lands late in the play (the runner is knocked back after it), so the ball comes
# out at the hit and the safety has a moment to reach it.
_RB_START = (0, -4)
_RB_PATH = [(0.8, -0.6), (1.6, 4.5), (1.0, 3.1)]
_HIT = share(_RB_START, _RB_PATH, 1)
FUMBLE = {
    "players": INTERIOR + [o("rb", "RB", *_RB_START), d("lb", "LB", 4, 6), d("s", "S", -4, 9)],
    "routes": [r("rb", "run", *_RB_PATH), r("lb", "blitz", (2.2, 5), (1.9, 4.8)),
               r("s", "blitz", (-1, 7), (2.4, 5.2))],
    "ball": ball(("rb", 0), (2.4, 5.2, round(_HIT + 0.03, 2)), ("s", 1.0)),
    "depth": 11, "behind": 6,
    "caption": "The ball comes loose, and the safety pounces on it",
}

# The receiving team runs up the screen; the kick comes in from the top.
KICK_RETURN = {
    "players": [o("kr", "KR", 0, 0), o("b1", "B", -5, 8), o("b2", "B", 5, 8),
                d("k1", "G", -8, 24), d("k2", "G", 0, 26), d("k3", "G", 8, 24)],
    "routes": [r("kr", "run", (0, 2), (3, 10), (2, 20)),
               r("b1", "block", (-4, 13)), r("b2", "block", (6, 13)),
               r("k1", "blitz", (-4, 14)), r("k2", "blitz", (-1, 17)), r("k3", "blitz", (6.5, 14))],
    "ball": ball((0, 28, 0), ("kr", 0.25)),
    "depth": 29, "behind": 3,
    "caption": "The returner catches the kick and runs it back behind blockers",
}

PUNT_RETURN = {
    "players": [o("pr", "PR", 0, 0), o("b1", "B", -6, 6), d("g1", "G", -12, 22), d("g2", "G", 12, 22)],
    "routes": [r("pr", "run", (0, 2), (-4, 8), (-9, 18)), r("b1", "block", (-7, 11)),
               r("g1", "blitz", (-7, 11)), r("g2", "blitz", (3, 8), (-3, 12))],
    "ball": ball((0, 26, 0), ("pr", 0.22)),
    "depth": 27, "behind": 3,
    "caption": "The punt returner catches it and runs it back",
}

FAIR_CATCH = {
    "players": [o("pr", "PR", 0, 0), d("g1", "G", -12, 22), d("g2", "G", 12, 22)],
    "routes": [r("g1", "blitz", (-2.5, 3.5)), r("g2", "blitz", (2.5, 3.5))],
    "ball": ball((0, 26, 0), ("pr", 0.7)),
    "depth": 27, "behind": 3,
    "caption": "Fair catch: the returner waves, catches, and nobody may hit them",
}

# The kicking team runs up the screen; the receiving team waits 10 yards away.
ONSIDE = {
    "players": [o("k", "K", 0, -1), o("c1", "G", -4, 0), o("c2", "G", 4, 0), o("c3", "G", 8, 0),
                d("r1", "R", 2, 11), d("r2", "R", 9, 12), d("r3", "R", -5, 12)],
    "routes": [r("c2", "run", (4.5, 6), (5.2, 11.4)), r("c3", "block", (8.6, 11)), r("r1", "blitz", (2.8, 13.6))],
    "ball": ball(("k", 0), (2.5, 5, 0.3), (5, 11.2, 0.8), ("c2", 1.0)),
    "zones": [zone("ten", "10 yards", -9, 10, 18, 0.6)],
    "depth": 15, "behind": 3,
    "caption": "Onside kick: short and bouncing, and the kicking team grabs it past 10 yards",
}

PLAYS = {
    "kid1": {
        "k1-scoring-01": TOUCHDOWN, "k1-downs-02": FIRST_DOWN, "k1-downs-03": PUNT, "k1-moving-01": SNAP,
        "k1-moving-02": HANDOFF, "k1-moving-03": PASS, "k1-moving-04": TACKLE, "k1-turnovers-02": INTERCEPTION,
        "k1-positions-03": BLOCKS, "k1-scoring-04": SAFETY, "k1-turnovers-03": FUMBLE,
    },
    "kid2": {"k2-runpass-01": HANDOFF, "k2-runpass-02": PASS, "k2-oline-02": PASS_PRO,
             "k2-kickoff-03": KICK_RETURN, "k2-punts-01": PUNT_RETURN, "k2-punts-02": FAIR_CATCH, "k2-onside-01": ONSIDE},
    "adult1": {"a1-scoring-01": TOUCHDOWN, "a1-scoring-02": SAFETY, "a1-downs-01": FIRST_DOWN,
               "a1-turnovers-01": INTERCEPTION, "a1-turnovers-02": PUNT},
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
