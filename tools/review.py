#!/usr/bin/env python3
"""Football sign-off for lesson steps (SPEC 13.3). Run from the repo root.

  python3 tools/review.py status
      Signed, stale and unsigned steps per level.
  python3 tools/review.py export --levels kid-1,kid-2,kid-3 [--notes notes.json] -o queue.json
      The review queue: every unsigned (or stale) step on those levels, in course order,
      with its text, its answer, its fingerprint and any pre-read note.
  python3 tools/review.py page queue.json -o page.html [--title "Kid Course Sign-off"] [--old-ids ids.json]
      The review page (tools/review-page.html) with the queue filled in, ready to publish.
  python3 tools/review.py apply decisions.json --reviewer "Name" [--season 2026] [--date YYYY-MM-DD]
      decisions.json is a list of {"step", "hash", "decision": "approve"|"flag", "note"}.
      Approvals whose fingerprint still matches the step are written to review.json;
      flags and stale approvals are listed for follow-up, never signed.

  python3 tools/review.py prune
      After editing signed steps: drop their now-stale sign-offs so they queue for review.

A step's fingerprint is the first 16 hex digits of SHA-256 over its canonical JSON
(keys sorted, no spaces, UTF-8). ReviewLogTest computes the same thing in Kotlin, so a
step edited after sign-off fails the build until it is reviewed again.
"""
import argparse, datetime, hashlib, json, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CONTENT = ROOT / "shared/core/src/commonMain/resources/content"
REVIEW = CONTENT / "review.json"


def fingerprint(step: dict) -> str:
    canon = json.dumps(step, sort_keys=True, separators=(",", ":"), ensure_ascii=False)
    return hashlib.sha256(canon.encode("utf-8")).hexdigest()[:16]


def steps():
    """(level, lesson, step) for every step, in course order."""
    cur = json.loads((CONTENT / "curriculum.json").read_text())
    for lv in cur["levels"]:
        for f in lv["unitFiles"]:
            for lesson in json.loads((CONTENT / f).read_text())["lessons"]:
                for s in lesson["steps"]:
                    yield lv, lesson, s


def load_review():
    return json.loads(REVIEW.read_text()) if REVIEW.exists() else {"version": 1, "signoffs": []}


def save_review(log):
    log["signoffs"].sort(key=lambda s: s["step"])
    REVIEW.write_text(json.dumps(log, indent=2, ensure_ascii=False) + "\n")


def state(step, signed):
    s = signed.get(step["id"])
    if s is None:
        return "unsigned"
    return "signed" if s["hash"] == fingerprint(step) else "stale"


def status(_):
    signed = {s["step"]: s for s in load_review()["signoffs"]}
    rows = {}
    for lv, _, s in steps():
        r = rows.setdefault(lv["id"], {"signed": 0, "stale": 0, "unsigned": 0})
        r[state(s, signed)] += 1
    for lid, r in rows.items():
        print(f"{lid:15} signed {r['signed']:4}  stale {r['stale']:3}  unsigned {r['unsigned']:4}")


def text(step):
    """What the reviewer reads: the step's words and its answer, without diagram data."""
    t = step["type"]
    out = {"type": t}
    for k in ("title", "body", "prompt", "statement", "explanation", "variant", "ruleSeason"):
        if k in step:
            out[k] = step[k]
    if t == "multiple_choice":
        out["choices"] = step["choices"]
        out["answer"] = step["answer"]
    elif t == "true_false":
        out["answer"] = step["answer"]
    elif t == "match":
        out["pairs"] = step["pairs"]
    elif t == "order":
        out["items"] = step["items"]
    elif t == "diagram_tap":
        labels = {p["id"]: p.get("label", p["id"]) for p in step["diagram"]["players"]}
        zones = {z["id"]: z.get("label", z["id"]) for z in step["diagram"].get("zones", [])}
        out["targets"] = [labels.get(x) or zones.get(x) or x for x in step["targets"]]
        out["targetIds"] = step["targets"]
    elif t == "situation":
        for k in ("options", "situation"):
            if k in step:
                out[k] = step[k]
    elif t == "read_defense":
        for k in ("choices", "answer", "disguised", "preSnap", "reveal"):
            if k in step:
                out[k] = step[k]
    if "diagram" in step:
        out["diagram"] = step["diagram"]
    if "scene" in step:
        out["scene"] = step["scene"]
    return out


def export(a):
    want = a.levels.split(",")
    notes = json.loads(Path(a.notes).read_text()) if a.notes else {}
    signed = {s["step"]: s for s in load_review()["signoffs"]}
    queue = []
    for lv, lesson, s in steps():
        if lv["id"] not in want:
            continue
        st = state(s, signed)
        if st == "signed":
            continue
        queue.append({
            "step": s["id"], "hash": fingerprint(s), "status": st,
            "level": lv["id"], "levelTitle": lv["title"], "lesson": lesson["id"], "lessonTitle": lesson["title"],
            "concept": s.get("concept"), "note": notes.get(s["id"]), **text(s),
        })
    queue.sort(key=lambda q: want.index(q["level"]))
    Path(a.o).write_text(json.dumps(queue, indent=1, ensure_ascii=False) + "\n")
    print(f"{len(queue)} steps to review -> {a.o}")


def apply(a):
    decisions = json.loads(Path(a.decisions).read_text())
    current = {s["id"]: s for _, _, s in steps()}
    log = load_review()
    signed = {s["step"]: s for s in log["signoffs"]}
    date = a.date or datetime.date.today().isoformat()
    added, flagged, stale = 0, [], []
    for d in decisions:
        sid = d["step"]
        if d["decision"] == "flag":
            flagged.append(d)
            continue
        if sid not in current or fingerprint(current[sid]) != d["hash"]:
            stale.append(sid)
            continue
        signed[sid] = {"step": sid, "reviewer": a.reviewer, "date": date, "ruleSeason": a.season, "hash": d["hash"]}
        added += 1
    log["signoffs"] = list(signed.values())
    save_review(log)
    print(f"signed {added}; flagged {len(flagged)}; changed since export (not signed) {len(stale)}")
    for d in flagged:
        print(f"  FLAG {d['step']}: {d.get('note') or ''}")
    for sid in stale:
        print(f"  STALE {sid}")


def page(a):
    """Fill tools/review-page.html with a queue (export's output) for publishing."""
    queue = json.loads(Path(a.queue).read_text())
    for q in queue:
        # The page reads a list of {kind, text}; a bare string would blank the whole page.
        note = q.get("note") or []
        q["note"] = [{"kind": "rule", "text": note}] if isinstance(note, str) else note
    old = set(json.loads(Path(a.old_ids).read_text())) if a.old_ids else None
    for q in queue:
        q["new"] = old is not None and q["step"] not in old
    def blob(v):  # safe inside <script type="application/json">
        return json.dumps(v, ensure_ascii=False).replace("</", "<\\/")
    html = (ROOT / "tools/review-page.html").read_text()
    html = html.replace("/*QUEUE*/", blob(queue)).replace("/*META*/", blob({"title": a.title}))
    if a.title:
        html = html.replace("<title>Kid Course Sign-off</title>", f"<title>{a.title}</title>", 1)
    Path(a.o).write_text(html)
    print(f"{len(queue)} steps -> {a.o}")


def prune(_):
    """Drop sign-offs whose step changed (or no longer exists): those steps need review again."""
    current = {s["id"]: s for _, _, s in steps()}
    log = load_review()
    keep, dropped = [], []
    for so in log["signoffs"]:
        st = current.get(so["step"])
        (keep if st is not None and fingerprint(st) == so["hash"] else dropped).append(so)
    log["signoffs"] = keep
    save_review(log)
    print(f"kept {len(keep)}; dropped {len(dropped)} changed since sign-off")
    for so in dropped:
        print(f"  {so['step']}")


def selftest(_):
    # Kotlin's ReviewLogTest checks the same value, so both sides hash alike.
    print(fingerprint({"b": [1, -3.2, True, None], "a": "Quote \" and \\ and é", "c": {"z": 0, "y": "line\nbreak"}}))


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = p.add_subparsers(dest="cmd", required=True)
    sub.add_parser("status").set_defaults(fn=status)
    e = sub.add_parser("export"); e.set_defaults(fn=export)
    e.add_argument("--levels", required=True); e.add_argument("--notes"); e.add_argument("-o", required=True)
    ap = sub.add_parser("apply"); ap.set_defaults(fn=apply)
    ap.add_argument("decisions"); ap.add_argument("--reviewer", required=True)
    ap.add_argument("--season", type=int, default=2026); ap.add_argument("--date")
    pg = sub.add_parser("page"); pg.set_defaults(fn=page)
    pg.add_argument("queue"); pg.add_argument("-o", required=True); pg.add_argument("--title", default="")
    pg.add_argument("--old-ids", help="JSON list of step ids that existed before; others are marked New")
    sub.add_parser("prune").set_defaults(fn=prune)
    sub.add_parser("selftest").set_defaults(fn=selftest)
    a = p.parse_args()
    a.fn(a)


if __name__ == "__main__":
    sys.exit(main())
