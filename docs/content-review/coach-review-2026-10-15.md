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

## Still to do

- Phone: finish Level 1 through the Checkpoint and Speed Round, then the read-the-defense
  reveals and pinch zoom on a Level 4+ profile.
- A human coach should still sign off the per-level lists in `level-2.md` … `level-8.md`.
- Product gaps a coach would ask for: a glossary screen, daily review, Level 0, and the
  Coach & Parent app (assignment of specific schemes).
