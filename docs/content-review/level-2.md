# Level 2: items for football review

AGENTS rule 7: kid-facing text must be right, and anything unsure is flagged rather
than guessed. Level 2 passed `ContentValidator`, but these items need a person with
coaching experience to sign off before a public release. Rule-dependent items say which
season they describe and must be re-checked every offseason (SPEC 3.5).

## Rules that change by season (re-check each offseason)

| Step | Claim | Why it needs review |
|---|---|---|
| `a2-replay-03` | NFL regular-season overtime: one 10-minute period, both teams get a possession unless the defense scores on the first, then next score wins; ties possible. Stated "as of the 2025 season". | Overtime rules changed in 2022 (playoffs) and 2025 (regular season). Confirm the current rule text. |
| `a2-special-03`, glossary `kickoff` | Kickoff from the 35; since 2024 the coverage team lines up close to the returners and nobody moves until the ball lands or is caught. | The kickoff format is new and still being tuned (touchback spot changed in 2025). |
| `a2-special-06`, glossary `onside-kick` | An NFL onside kick must be declared in advance. | Onside rules changed in 2024 and 2025 (who may declare and when). |
| `a2-replay-01`, `a2-replay-02`, glossary `challenge` | Two challenges, a third if both succeed; a lost challenge costs a timeout; booth-initiated reviews inside two minutes and in overtime. | Replay assist and review rules are amended most years. |
| `a2-protect-03`, glossary `targeting` | Targeting is a college rule (15 yards and ejection); the NFL penalizes helmet hits under its own rules. | Confirm NCAA wording and the NFL's current helmet rule names. |
| `k2-clock-01`, `a2-gameday-*` | NFL teams have three timeouts per half. | Stable, but youth/flag rules differ (the content says so). |

## Simplifications for kids (deliberate, confirm they're acceptable)

| Step | Simplification |
|---|---|
| `k2-onside-02` | "The ball must go 10 yards before the kicking team can grab it." Omits the exception that the kicking team may recover once the receiving team touches it. |
| `k2-kickoff-07` | Touchback: "their team starts from a set spot". The spot differs by league and has changed recently, so no yard line is given. |
| `k2-clock-02` | Clock stops "for timeouts, scores and penalties". Omits running-clock exceptions after penalties and out-of-bounds timing outside the last minutes. |
| `k2-oline-06` | "In the NFL, linemen usually aren't allowed to catch passes." Omits reporting as eligible (taught at adult Level 2). |

## Judgement calls in Game Day situations

"Best" is what most modern coaches and win-probability models would choose. These are
the closest calls; a coach should agree with the labels and the "why" text.

| Step | Call | Note |
|---|---|---|
| `a2-gameday-03` | Down 7, 3:00 left, three timeouts: kick deep is best, onside is poor. | Some would rate onside "ok" rather than "poor". |
| `a2-gameday-08` | Up 6 in the first quarter: extra point best, two-point "ok". | Expected points are nearly equal; labelled so neither is "poor". |
| `a2-gameday-10` | 4th and 1 at own 45, tied, second quarter: go for it is best, punt "ok". | Matches fourth-down models; traditional coaches punt. |
| `a2-gameday-12` | Up 4, 0:58 left, 2nd and 8 at own 40, they have 3 timeouts: run is best, kneel "ok". | Reasonable people differ on kneel vs run here. |

Sign-off: record reviewer, date and rule season per step in `review.json` once that
file exists (SPEC 3.7).
