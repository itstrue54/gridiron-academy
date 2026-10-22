#!/usr/bin/env python3
"""One-off (launch fixes, phase 6): kid levels and kid glossary definitions say "they"
for players, since kids' football is played by boys and girls. Exact whole-string
replacements, so each one was read by a person. Run from the repo root."""
import json
from pathlib import Path

C = Path("shared/core/src/commonMain/resources/content")
R = {
    "A safety is worth 2 points. The defense gets it by tackling the ball carrier in his own end zone.":
        "A safety is worth 2 points. The defense gets it by tackling the ball carrier in their own end zone.",
    "Tackle him to the ground": "Tackle them to the ground",
    "Pull his flag": "Pull their flag",
    "Tag him with one hand": "Tag them with one hand",
    "A blocker grabs a defender's jersey to stop him. What penalty is that?":
        "A blocker grabs a defender's jersey to stop them. What penalty is that?",
    "In the NFL, once a receiver is 5 yards past the line, a defender may not push or grab him before the ball is thrown. That's illegal contact. High school rules are different, so ask your coach.":
        "In the NFL, once a receiver is 5 yards past the line, a defender may not push or grab them before the ball is thrown. That's illegal contact. High school rules are different, so ask your coach.",
    "Defenders may not hit the quarterback late, once the ball is clearly gone, or hit him in the head. That's roughing the passer: 15 yards and a first down.":
        "Defenders may not hit the quarterback late, once the ball is clearly gone, or hit them in the head. That's roughing the passer: 15 yards and a first down.",
    "A defender hits the quarterback after he already threw the ball. What's the call?":
        "A defender hits the quarterback after the ball is already thrown. What's the call?",
    "It's fine to tackle a player after the whistle if he still has the ball.":
        "It's fine to tackle a player after the whistle if they still have the ball.",
    "On runs, linemen push defenders out of the way. On passes, they protect the quarterback so he has time to throw.":
        "On runs, linemen push defenders out of the way. On passes, they protect the quarterback so there's time to throw.",
    "The five linemen stand side by side in the middle: the center and the guards and tackles next to him.":
        "The five linemen stand side by side in the middle: the center, with the guards and tackles next to the center.",
    "The quarterback can stand right behind the center (under center) or about five yards back. Standing back is called the shotgun. It gives him more time to see the defense.":
        "The quarterback can stand right behind the center (under center) or about five yards back. Standing back is called the shotgun. It gives more time to see the defense.",
    "Singleback has one running back behind a quarterback under center. In the pistol, the quarterback stands about four yards back and the running back stands right behind him.":
        "Singleback has one running back behind a quarterback under center. In the pistol, the quarterback stands about four yards back and the running back stands right behind the quarterback.",
    "The quarterback is about five yards back with a running back beside him: shotgun.":
        "The quarterback is about five yards back with a running back alongside: shotgun.",
    "The quarterback is a few yards back and the running back is right behind him: the pistol.":
        "The quarterback is a few yards back and the running back is right behind: the pistol.",
    "A route is the path a receiver runs on a pass play. The quarterback knows every route, so he knows where to throw.":
        "A route is the path a receiver runs on a pass play. The quarterback knows every route, so they know where to throw.",
    "On a go route, the receiver sprints straight down the field as fast as he can. It is the deep shot.":
        "On a go route, the receiver sprints straight down the field as fast as they can. It is the deep shot.",
    "The quarterback watches the defender. If he stays deep, throw short. If he comes up, throw deep.":
        "The quarterback watches the defender. If they stay deep, throw short. If they come up, throw deep.",
    "That is the whole idea: read the defender, throw away from him.":
        "That is the whole idea: read the defender, throw away from them.",
    "If the defender comes up, the deep receiver is behind him and open.":
        "If the defender comes up, the deep receiver is behind them and open.",
    "In man coverage, each defender is assigned one receiver and follows him wherever he goes.":
        "In man coverage, each defender is assigned one receiver and follows them wherever they go.",
    "The cornerback lined up across from X follows him on the route.":
        "The cornerback lined up across from X follows X on the route.",
    "That is zone coverage. In man coverage, a defender follows his receiver wherever he goes.":
        "That is zone coverage. In man coverage, a defender follows their receiver wherever they go.",
    "Look where the defender stands. If he's inside the receiver, closer to the middle, a route toward the sideline is easier. If he's outside, a route to the middle is easier. That's leverage.":
        "Look where the defender stands. If they're inside the receiver, closer to the middle, a route toward the sideline is easier. If they're outside, a route to the middle is easier. That's leverage.",
    "X's defender stands inside him, guarding the middle, so going toward the sideline is easier.":
        "X's defender stands inside X, guarding the middle, so going toward the sideline is easier.",
    "In zone coverage, each defender guards an area of the field. He covers any receiver who comes into his area.":
        "In zone coverage, each defender guards an area of the field. They cover any receiver who comes into their area.",
    "That's man coverage. In zone, he stays in his area.":
        "That's man coverage. In zone, each defender stays in their area.",
    "The running back carries the ball around the end. The linemen are blocking for him.":
        "The running back carries the ball around the end. The linemen are blocking for the runner.",
    "The pass rushers charge at the quarterback, then he hands off and the runner goes right past them.":
        "The pass rushers charge at the quarterback, who hands off, and the runner goes right past them.",
    "On some runs, a guard leaves his spot at the snap and runs sideways along the line to block in front of the runner. That's called pulling.":
        "On some runs, a guard leaves their spot at the snap and runs sideways along the line to block in front of the runner. That's called pulling.",
    "He kneels, the play ends safely, and the clock keeps running.":
        "The quarterback kneels, the play ends safely, and the clock keeps running.",
    "An NFL coach who thinks a call was wrong can throw a red flag to challenge it. If the call stands, his team loses a timeout.":
        "An NFL coach who thinks a call was wrong can throw a red flag to challenge it. If the call stands, the team loses a timeout.",
    "A coach throws a red flag onto the field. What does he want?":
        "A coach throws a red flag onto the field. What does the coach want?",
    "A timeout for his kicker": "A timeout for the kicker",
    "A penalty on his own team": "A penalty on their own team",
    # Glossary, kid definitions
    "When the defense tackles the ball carrier in his own end zone. Worth 2 points.":
        "When the defense tackles the ball carrier in their own end zone. Worth 2 points.",
    "The quarterback is a few yards back with the runner right behind him.":
        "The quarterback is a few yards back with the runner right behind.",
    "Hitting the quarterback after he throws.": "Hitting the quarterback after the ball is thrown.",
    "A lineman leaves his spot to block elsewhere.": "A lineman leaves their spot to block somewhere else.",
    "Tackling the quarterback before he throws.": "Tackling the quarterback before the ball is thrown.",
    "The quarterback reads a defender instead of blocking him.": "The quarterback reads a defender instead of blocking them.",
    "The order a quarterback reads his receivers.": "The order a quarterback reads their receivers.",
}

used = set()
def fix(o):
    if isinstance(o, dict): return {k: fix(v) for k, v in o.items()}
    if isinstance(o, list): return [fix(v) for v in o]
    if isinstance(o, str) and o in R: used.add(o); return R[o]
    return o

for path in sorted(C.glob("kid*/*.json")) + [C / "glossary.json"]:
    data = json.loads(path.read_text())
    new = fix(data) if path.name != "glossary.json" else {
        **data, "entries": [{**e, "kid": R.get(e["kid"], e["kid"])} for e in data["entries"]]}
    if path.name == "glossary.json":
        used.update(e["kid"] for e in data["entries"] if e["kid"] in R)
    if new != data: path.write_text(json.dumps(new, indent=2, ensure_ascii=False) + "\n")
assert used == set(R), set(R) - used
print(f"replaced {len(used)} strings")
