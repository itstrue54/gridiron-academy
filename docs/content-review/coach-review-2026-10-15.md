# Coach review, 2026-10-15

A read of all eight levels as an NFL coach would read them, plus a phone run on the S24.

## How it was done

- **Level 1:** every step read by hand against the NFL rulebook.
- **Levels 2–8:** all 663 steps triaged with `tools/content-triage.py`, which asks the Jev
  model per step "would a veteran NFL coach flag this?". The top 42 were then read by hand.
- **Calibration warning:** Jev scored the known Level 1 eligibility bug at only 0.22, and
  its scores drift about ±0.1 between runs. Treat its ranking as a reading order, never a
  sign-off. Steps below the top 42 have not had a coach's eye since they were written.
- **Phone:** an autoplayer ran Level 1 lessons on the adult profile. No crashes.

## Fixed

| Step | Problem | Fix |
| --- | --- | --- |
| `a1-positions-06` | Counted a covered TE and an under-centre QB as eligible | Alignment-correct diagram, key and teaching card |
| Match steps (all) | Tapping the pre-selected first item deselected it, and Check stayed disabled | A tap always selects |
| `k1-positions-06` | "Who does what on defense?" included OL | Prompt "Who does what?", explanation names which side each plays |
| `p6-presnap-03` | Said "inside the tight end: six", but the end over the TE is one of the six | "from the end over the tight end inward" |
| `co7-pressure-03/04/05`, `co7-late-03`, `co7-film-04` | "Bluff: four rush, seven cover" also describes the keyed sim pressure | "Bluff: shows pressure, the usual four rush" |
| `co7-rotate-05`, `co7-film-02` | Cover 1 reveal put two defenders on F and none in the hole | The rotating safety drops to the hole as a robber |
| Situation scoreboard | "3 timeouts left" next to a prompt saying "they have no timeouts" | Label reads "Your timeouts: 3" |

## Checked and correct (high Jev score, no change)

Penalty yardages, OT rules as of 2025, the eligible-number rule (50–79), Over/Under,
Okie/Tite, the 46 Bear, counter/trap/duo, the Smash and Snag concepts, Cover 0/1/3 cloud/
Tampa 2/buzz/Cover 6, fire zone, overload, the speed-option and zone-read keys, the clock
decisions (kneel, spike, field-goal distances), and the coverage-beater match.

## Phone run (S24, adult profile)

- Level 1: all six drills, Checkpoint 100%, Speed Round 100% (streak 10).
- Levels 2, 3 and 4 by test-out: every Checkpoint and every Big Game type (Game Day,
  Diagram Dash, Read the Defense) played through and passed. Level 5 unlocked.
- Read the Defense: the reveal plays with ghosts, blitz and drop paths, and "Replay the
  snap". The zoom buttons work, and dragging pans the zoomed field. A two-finger pinch
  can't be sent over adb, so pinch itself still needs a finger test.
- Fixed on the way: the result screen centred its content on the widest line rather than
  the screen, and "Level N complete" said the next level "opens as soon as it's
  published" even when it was already open. It now names the level that opened.
- No crashes in logcat.

## Full coach read, 2026-10-16

Every step in Levels 1-8 and both preview units was read as a coach would read it:
the text, the answer key, every distractor and every diagram. Rules, terminology,
coverages, fronts, run schemes, pass concepts, clock and 4th-down decisions and the
analytics all held up. What didn't:

| Step(s) | Problem | Fix |
| --- | --- | --- |
| `co7-late-02`, `co7-late-04` | Z motioned off the line, leaving six on the line at the snap: an illegal formation | F on the line, Z a flanker; the validator now rejects a full offense with fewer than 7 on the line |
| `a2-eligibility-02` | "Every player in the backfield is eligible" contradicted Level 1's under-centre QB rule | Names the NFL exception |
| `k1-positions-07..10`, `v4-personnel-02/03/05`, `v4-strength-01/03`, `p6-presnap-02..04`, preview `p7-disguise-02/03` | A receiver on the line outside the tight end covered him, making the TE ineligible in pictures meant to show TE personnel | The outside receiver is a flanker, off the ball |
| `k2-oline-01/03/04`, `a2-eligibility-04` | Ten offensive players, in lessons about the line and about counting eligibles | Eleventh player added; explanation names him |
| `k1-refs-05` | "A player moves too early" keyed false start, but a defender doing it is offside | "A player on offense" |
| `a3-twominute-02` | "Late in each half" for the out-of-bounds clock rule | Last two minutes of the first half, last five of the game |
| `v4-gap-02`, `v4-gap-05` | Power drawn without the center blocking back or the TE blocking down | Both blocks drawn |
| `v4-dropback-04/07` | Y-cross run by a player labelled H | Labelled Y |
| `co7-late-01`, `co7-late-03` | The walkdown blitz path stopped 3 yards short of the line | Ends in the backfield |
| `c5-motion-03` | Return motion stopped 3 yards short of where it began | Returns to the start |

Left as is, on purpose: about 25 pass-concept and protection diagrams draw only the
players the concept uses (9-10 on offense), which is how coaches draw on a whiteboard.

## Still to do

- A human coach should still sign off the per-level lists in `level-2.md` … `level-8.md`.
- Product notes from the run:
  - ~~Zoomed in one step, the corners leave the frame.~~ Done 2026-10-19: no zoom step
    can keep them (measured); players out of view now get edge markers (DESIGN 7.10).
  - ~~One tap on ✕ leaves a 12-question Checkpoint without asking.~~ Done 2026-10-19:
    ✕ and Back ask once something's been answered (DESIGN 9.3).
  - A glossary screen, daily review, Level 0, and the Coach & Parent app.
