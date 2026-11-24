# Learner app audit fixes: design

Date: 2026-11-24. Owner: itstrue54. Source: a three-part read-only audit of the learner app
(logic, UI/accessibility, content as data), top findings checked against code and renders.

## Intent

Fix what the audit found before more learners arrive: work that silently disappears, a streak
that misreports, screens that break at large text or under TalkBack, finish screens that are
harsh on a 6-year-old, wasted frame work in diagram animations, thin spots in the course,
and a SPEC that promises more than exists. Owner picked all seven items.

**Goals:** each item lands as its own PR off `main`, with tests, SPEC updated in the same PR
(AGENTS.md rule 8). **Non-goals:** new step types, new games, Level 0, stickers/badges (the
docs PR only marks them as planned). **Anti-goals:** any change to what a correct answer is
in signed-off content without the owner re-signing it; any visual change in PR E.

## Approaches considered

- **Seven independent PRs off main** (chosen): each reviewable and mergeable alone. Cost:
  `docs/SPEC.md` and `strings.xml` are touched by several, so later merges need a rebase.
- Stacked PRs: no rebases, but merging one deletes the base under the next (a closed PR has
  happened here before), and one stuck PR blocks the rest.
- One big PR: least overhead, impossible to review or to merge in part.

## The PRs

### A. `keep-work`: don't lose a kid's work
1. Pending graded answers are written when the app leaves the screen: a public
   `LearnerAppState.saveNow()` (commits if `pending` is non-empty), called from
   `MainActivity.onStop` before `leftTheApp()`.
2. Leaving practice part-way applies the answered items (cards answered move, the day
   counts toward the streak). Practice stays on Home while any review card is still due;
   `DailyPractice.doneToday` becomes "practised today and nothing is due".
3. Checkpoint/Big Game seed adds the number of today's `CHECK`/`GAME` attempt events for that
   level's steps (saved + pending), so a retake after a restart draws a new set. No schema change.
4. `Review.answered` moves a card up only when it is due (`dueDay <= today`); a miss on a
   not-due card still sends it to box 1.
5. Time counted for one step is capped (`MAX_STEP_MS`, named constant, 5 minutes), so a
   question left open overnight doesn't add hours to "minutes this week".

### B. `honest-streak`: a streak that tells the truth
- Pure `Streak` functions in `:shared:core` (time passed in as epoch day):
  `current(progress, today)` is what Home shows; it is 0 once the streak has lapsed.
- **Rest day (SPEC 4.7):** one missed day per week (Monday-Sunday, from epoch day) is
  forgiven automatically: a gap of exactly one missed day keeps the streak if that week's
  rest is unused, and uses it. The rest day itself doesn't add to the count.
- A date earlier than `lastActiveDay` (clock set back, travel west) changes nothing.
- Needs `Progress.restWeek` (the week whose rest day was used): **save schema 3** with a
  migration in `SaveMigrations` and a fixture test (SPEC 9.1). Home's chip says when the
  rest day saved the streak. Coach app display is out of scope (it reads synced data later).

### C. `big-text-a11y`: big text and TalkBack
- Player-mark labels sized from the mark (dp), not scaled again by font scale.
- Situation info row wraps (FlowRow); feedback sheet scrolls with a visible cue; Playbook map
  rows grow with font scale and never break a word; letter chips, timer ring and the
  "More below" pill (48dp target) hold 200% text.
- List mode shows a missed target as missed (not "correct") to sight and TalkBack.
- Match board: paired items look paired (not the "selecting" gold), TalkBack says "paired
  with X", tapping a right item first gives a hint, the hint matches the stacked layout.
- Focus moves to Continue after Check; Order step announces the new position; search fields
  have labels; Speed round shows right/wrong on screen, not only as a sound.
- Checked with the existing 200% render set (`f2-*`) and new renders where needed.

### D. `kid-finish`: kind finish screens
- Below the pass mark, "Try again" is the main button; the XP line is hidden at +0.
- After passing a level, a "Next level" button opens it.
- Tapping a locked level says what opens it.
- Kid copy: `ok_kid`, `poor_kid` rewritten for ages 6-9; kid gate blurbs say "10 of 12"
  instead of percentages. Adult copy unchanged.

### E. `smooth-diagrams`: animation cost
- Animation progress is read in the draw phase (lambda), not in composition; `spoken()`
  remembered per diagram; draw-loop allocations (Paint, TextStyle, groupBy, indexOf) hoisted;
  Speed round choice order remembered per question.
- **Must be pixel-identical:** every PNG in `field-renders/` and `screen-renders/` byte-equal
  to `main`'s.

### F. `content-round`: course content (owner re-signs; release holds changed levels until then)
1. Gates may set `"ordered": true`; the game then plays its pool in authored order.
   Architect-8's whole-game sim uses it.
2. Game-only items: a gate may carry its own `gameItems`, drawn before lesson items. First
   use: 8 new game-day situations each for adult-2 and college-5, so the Big Game isn't a
   replay of the one game-day lesson. (Read-the-defense gates later; they need diagrams.)
3. Kid ladder grades motion, double-team, audible, disguise and the two-point try (about two
   items each, at or above each term's glossary level). Validator: a prerequisite must be
   graded on the same ladder no later than the concept.
4. Two more graded items on each ladder for the L1 basics that have one: field, defense,
   snap, tackle, turnover, eligible receiver.
- New step ids only (SPEC 3.4). Kid3 "Ready for Varsity" stays untagged (ADR-010).
- A private review page from the branch (`tools/review.py export/page`) for sign-off.

### G. `spec-honesty`: docs
- SPEC 4.2/4.3/4.7/6.2/6.9 say plainly what is built and what is planned (new step types,
  five games, Level 0, stickers/badges, lesson resume). This design doc is committed here.

## Left to the builder
Exact kid wording (D), the visual treatment of "paired" and "missed" (C), the rest-day chip
text (B), and the football content of F (marked for the owner's review, as always).
