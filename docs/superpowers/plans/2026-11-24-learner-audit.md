# Learner Audit Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Seven independent PRs off `main` that fix what the 2026-11-24 learner audit found.

**Architecture:** Each PR is one branch from `origin/main` in its own git worktree. Logic lives in
`:shared:core` (pure KMP, injected time and `Rng`); `LearnerAppState` wires it; Compose screens
in `:shared:ui` only render. Content is JSON under `shared/core/src/commonMain/resources/content`.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform 1.9.3, kotlinx.serialization, Gradle.

**Spec:** `docs/superpowers/specs/2026-11-24-learner-audit-design.md`

Paths: `C` = `shared/core/src/commonMain/kotlin/io/github/itstrue54/gridironacademy`,
`U` = `shared/ui/src/commonMain/kotlin/io/github/itstrue54/gridironacademy/ui`,
`STR` = `shared/ui/src/commonMain/composeResources/values/strings.xml`.

## Global Constraints

- AGENTS.md rules apply: no `android.*`/`java.*` in commonMain; no clocks or `kotlin.random` in `:shared:core`; models are `data class` with `val`; lesson text only in JSON.
- Verify with `./gradlew :shared:core:jvmTest :shared:ui:jvmTest` (green) before every commit that ends a PR; never weaken or delete a failing test.
- Update `docs/SPEC.md` (and `docs/CURRICULUM.md` for content) in the same PR as the behaviour.
- Commit prefixes `feat: fix: test: docs: content: refactor: chore:`; git identity itstrue54 / `itstrue54@users.noreply.github.com`; end commits with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Compose resource strings: plain `'` and `%`, no Android escapes.
- New step ids only; never reuse or rename one (SPEC 3.4).
- Do not push or open PRs from a worker; the coordinator does that.

## Review Focus

1. A save written by this build read back by itself and by `main`'s decoder rules: schema 3 must decode a v1 and v2 fixture, and a v3 save must be refused by a v2 app (`SaveTooNew`) — test in Task B2.
2. The app killed while the Leave dialog is up, or right after the last answer before the done screen: pending answers must already be on disk — test in Task A1 (`saveNow` after answers, before finish).
3. Midnight between opening practice and finishing it: the finish uses one `today()` for card moves and `touch` — test in Task A2 (fixed day function advanced mid-session).
4. Font scale 2.0 on a 360dp-wide screen for every step type: no text clipped or overlapping — renders in Task C1/C2 at 360dp width.
5. A level whose gate pool is game-only items plus lesson items smaller than `gameQuestions`: the validator must still refuse an unplayable gate — test in Task F1.

---

## PR A — branch `keep-work`

### Task A1: Save answers when the app leaves the screen

**Files:** Modify `U/app/LearnerAppState.kt` (near `back()`, ~555), `androidApp-learner/src/main/kotlin/io/github/itstrue54/gridironacademy/learner/MainActivity.kt:86-91`; Test `shared/ui/src/jvmTest/.../ui/LearnerAppStateTest.kt`

**Interfaces:** Produces `fun LearnerAppState.saveNow()` — commits `save` when `pending` is non-empty, otherwise no-op.

- [ ] Test `saveNowWritesPendingAnswers`: start a lesson, answer 3 graded steps, call `saveNow()`; a fresh `LearnerAppState` on the same store sees 3 attempts. Test `saveNowWithNothingPendingDoesNotWrite`: store write count unchanged.
- [ ] Run, see both fail (no `saveNow`).
- [ ] Implement `saveNow()`; call it from `MainActivity.onStop` before `leftTheApp()`, also when `isChangingConfigurations`.
- [ ] Run green; commit `fix: answers are saved when the app leaves the screen`.

### Task A2: Leaving practice part-way applies what was answered

**Files:** Modify `U/app/LearnerAppState.kt` (`finishPractice` ~327, `back()`), `C/progress/DailyPractice.kt:71`; Test `LearnerAppStateTest.kt`, `shared/core/src/jvmTest/.../DailyPracticeTest.kt` (create if absent)

**Interfaces:** Produces `DailyPractice.doneToday(attempts: List<AttemptEvent>, progress: Progress, today: Long): Boolean` = any REVIEW attempt today **and** `Review.due(progress, today).isEmpty()`. Private `applyPractice(answered: List<Step>, missedIds: Set<String>)` shared by finish and leave.

- [ ] Tests: `leavingPracticeMovesAnsweredCardsOnly` (2 due cards, answer the first right, leave → first moves to box 2, second unchanged); `leavingPracticeCountsForStreak` (`lastActiveDay == today`); `practiceNotDoneWhileCardsDue`; `practiceDoneWhenNothingDue`; `practiceAcrossMidnightUsesOneDay`.
- [ ] Run, see them fail.
- [ ] Implement; `back()` from `PracticePlay` with answered steps calls `applyPractice` for the answered ones (unanswered stay as they were). Update every caller of `doneToday`.
- [ ] Green; commit `fix: leaving practice part-way keeps what was answered`.

### Task A3: Checkpoint and Big Game retakes draw new questions after a restart

**Files:** Modify `U/app/LearnerAppState.kt:241-250`; Test `LearnerAppStateTest.kt`

- [ ] Test `checkpointRetakeAfterRestartDrawsNewSet`: open Checkpoint, answer 12, finish (fail); new `LearnerAppState` on same store and same day; reopen → question id list differs. Same for Big Game.
- [ ] Run, fails.
- [ ] Replace in-memory `gateAttempts` with a count of today's `CHECK` (Checkpoint) or `GAME` (Big Game) events whose `stepId` belongs to the level, in `save.attempts + pending`. Keep the seed formula otherwise.
- [ ] Green; commit `fix: a gate retake after a restart asks new questions`.

### Task A4: Only due cards move up; step time capped

**Files:** Modify `C/progress/Review.kt:14-20`, `U/app/LearnerAppState.kt:~796`; Tests `ReviewTest.kt` (core), `LearnerAppStateTest.kt`

**Interfaces:** `Review.answered` unchanged signature; a correct answer on a card with `dueDay > today` returns `p` unchanged. `const val MAX_STEP_MS = 300_000` in `LearnerAppState` companion (UI layer: it measures wall time).

- [ ] Tests: `correctOnNotDueCardDoesNotMove`, `missOnNotDueCardGoesToBoxOne`, `correctOnDueCardMovesUp` (existing behaviour kept); `stepTimeIsCappedAtFiveMinutes` (inject `nowMillis` jumping 8 h; recorded `msSpent == 300_000`).
- [ ] Fail, implement, green.
- [ ] Update SPEC 6.4 (practice done rule, due-only moves) and 9 (answers saved on leaving the screen); commit `fix: only due cards move up, and one step counts at most five minutes`.

## PR B — branch `honest-streak`

### Task B1: `Streak` rules in core

**Files:** Create `C/progress/Streak.kt`; Modify `C/progress/Progress.kt` (`touch`, new field); Test `shared/core/src/jvmTest/.../StreakTest.kt`

**Interfaces:** `Progress.restWeek: Long = -1`. `object Streak { fun weekOf(day: Long): Long; fun current(p: Progress, today: Long): Int; fun restUsedThisWeek(p: Progress, today: Long): Boolean }`. `weekOf` = Monday-based week index (epoch day 0 was a Thursday: `Math.floorDiv(day + 3, 7)` written with KMP-safe floor division). `Progress.touch(today)` rules:
  - `today <= lastActiveDay` → unchanged (same day or clock set back).
  - `today == lastActiveDay + 1` → streak + 1.
  - `today == lastActiveDay + 2` and `restWeek != weekOf(lastActiveDay + 1)` and streak > 0 → streak + 1, `restWeek = weekOf(lastActiveDay + 1)` (rest day doesn't add).
  - otherwise → streak 1.
  - `current`: `streakDays` if `today - lastActiveDay <= 1`, or `== 2` with the rest day still available for that missed day's week; else 0.
- [ ] Tests (named for the rule): `sameDayNoChange`, `nextDayAddsOne`, `oneMissedDayUsesRest`, `secondMissedDayInSameWeekBreaks`, `restRefreshesNextWeek`, `twoMissedDaysBreak`, `clockBackNoChange`, `currentShowsZeroAfterLapse`, `currentKeepsStreakDuringRestDay`, `weekOfMondayBoundary` (epoch day 4 = Monday 1970-01-05 starts a new week).
- [ ] Fail, implement, green; commit `feat: one rest day a week, and the streak shows zero once it lapses`.

### Task B2: Save schema 3

**Files:** Modify `C/progress/LearnerSave.kt` (`CURRENT_SCHEMA = 3`), `shared/data/.../migration/SaveMigrations.kt`; Test the existing migration test file plus a v2 fixture beside the v1 fixture.

- [ ] Tests: `v2FixtureMigratesToV3` (restWeek -1, everything else equal), `v1FixtureMigratesToV3`, `v4SaveIsRefused`.
- [ ] Fail; add `v2to3` (sets `schemaVersion` 3; field defaults); green; commit `feat: save schema 3 for the rest day`.

### Task B3: Home shows the true streak

**Files:** Modify `U/screens/HomeScreen.kt:104` (`StreakChip`), `STR`; Test `ScreenRenderTest` adds a render `03c-home-streak-rest-kid`.

- [ ] Home passes `Streak.current(progress, today)`; when `restUsedThisWeek` and the rest covered yesterday, the chip adds a short line (builder's wording, e.g. "Rest day used").
- [ ] Render test + `LearnerAppStateTest.homeStreakLapsedShowsStart`. Update SPEC 4.7 (rest day built) and 9 (schema 3). Green; commit `feat: Home shows the streak as it stands today`.

## PR C — branch `big-text-a11y`

### Task C1: Diagrams and HUD at 200% text

**Files:** `U/field/FieldDiagram.kt:196-202,567`, `U/lesson/StepViews.kt:562`, `U/components/Components.kt:175,266-269`, `U/screens/PlaybookMapScreen.kt:316-320,440`, `U/games/SpeedRoundScreen.kt:254,263`, `U/lesson/LessonScreen.kt:258-263`; Test `ScreenRenderTest` (`f2-*` set, add 360dp-wide 200% renders for situation, feedback sheet, map, speed round).
- [ ] Mark label size from dp (divide the sp value by `fontScale`) so it scales with the mark only; situation info row → `FlowRow`; feedback sheet scrollable with a fade/arrow cue when more text below; map row height from measured label height, `softWrap` without mid-word breaks (hyphen-free: shrink then wrap at spaces only); letter chip and timer ring sized from text (`widthIn(min=)`); "More below" pill min 48dp.
- [ ] Look at every new and changed render with Read; no clipping or overlap. Green; commit `fix: diagrams, the situation line and the map hold 200% text`.

### Task C2: TalkBack and the Match board

**Files:** `U/lesson/StepViews.kt:302-306,326-405,453-454`, `U/lesson/LessonScreen.kt:219-227`, `U/games/SpeedRoundScreen.kt:98,161`, `U/screens/GlossaryScreen.kt:53`, `U/screens/PlaybookMapScreen.kt` (search), `U/field/FieldDiagram.kt:84,131` (`FieldLabels` from strings), `STR`; Tests `shared/ui/src/jvmTest/.../StepViewsUiTest.kt` (create; compose ui-test like `GrownUpZoneUiTest`).
- [ ] Tests: `listModeMissedTargetIsNotCorrect` (semantics state words contain the "missed" string), `matchPairedItemsSayPaired`, `matchRightFirstShowsHint`, `continueGetsFocusAfterCheck`, `searchFieldHasLabel`, `speedRoundShowsVerdictText`.
- [ ] Implement: `AnswerState.MISSED` (or reuse RETRY with "missed" words) for an untapped target; `MatchChip` uses `paired` (distinct look, not SELECTED gold; "paired with X"); hint string per layout; `FocusRequester` on Continue; order step announces "now N of M" via `liveRegion`; `label` on search fields; Speed round shows a short on-screen verdict; zoom and LOS labels from strings.
- [ ] Green; SPEC 10 note; commit `fix: TalkBack keeps its place, and the Match board says what's paired`.

## PR D — branch `kid-finish`

### Task D1: Finish and level screens

**Files:** `U/screens/` lesson-done screen (the screen behind render `15-done-kid-retry`), `U/screens/GateDoneScreen.kt:~150`, `U/screens/HomeScreen.kt:258`, `U/app/LearnerAppState.kt` (`openNextLevel()`), `STR`; Tests `LearnerAppStateTest.kt`, `ScreenRenderTest`.

**Interfaces:** `fun LearnerAppState.nextLevelAfter(level: Level): Level?`; `fun openLevel(level)` already exists — reuse.
- [ ] Tests: `nextLevelAfterPassedLevel`, `nextLevelNullAtTop`; renders `15-done-kid-retry` (Try again primary, no XP line at 0), `21-level-complete-kid` (Next level button), new `03d-home-locked-tap-kid` (message naming what opens it).
- [ ] Implement; kid strings `ok_kid`, `poor_kid` rewritten (ages 6-9, short, kind), `checkpoint_blurb`/`game_not_yet_kid` say "N of M" (formatted from the gate). Adult strings untouched.
- [ ] Green; SPEC 6 note; commit `feat: kinder finish screens for kids, and a Next level button`.

## PR E — branch `smooth-diagrams`

### Task E1: Animation work in the draw phase, pixel-identical

**Files:** `U/lesson/StepViews.kt:160-170,490-500`, `U/field/FieldDiagram.kt:240-265,202`, `U/games/SpeedRoundScreen.kt:161-165`.
- [ ] Before changing code: run the UI tests on the branch and copy `shared/ui/build/field-renders` and `screen-renders` to a scratch folder (baseline).
- [ ] `FieldDiagram` takes `motion: () -> Float` read inside the Canvas draw; `remember(diagram) { diagram.spoken() }`; per-diagram derived data (`groupBy`, `associateBy`, index maps) in `remember(diagram)`; `Paint`/`TextStyle` remembered and only re-made when size inputs change; Speed round `choiceOrder` in `remember(question.id)`.
- [ ] Run UI tests; `cmp` every PNG against the baseline — all identical (any diff is a bug to fix, not to accept).
- [ ] Commit `refactor: diagram animations stop recomposing every frame`.

## PR F — branch `content-round`

### Task F1: Ordered games and game-only items

**Files:** `C/model/Curriculum.kt:29-37` (`LevelGate.ordered: Boolean = false`, `gameItems: List<Step> = emptyList()`), `C/engine/LevelTest.kt`, `C/content/ContentValidator.kt`, `curriculum.json` (architect-8 `"ordered": true`); Tests `LevelTestTest.kt`, `ContentValidatorTest.kt`.
- [ ] Tests: `orderedGamePlaysAuthoredOrder` (first `gameQuestions` of the pool, in order, any seed); `gameItemsDrawnFirst` (pool of 8 game items + lesson items, 10 questions → all 8 game items present); `gateUnplayableWithTooFewItemsRejected`; `gameItemsValidatedLikeSteps` (missing explanation rejected).
- [ ] Implement; `ReleaseContent` and review fingerprints include `gameItems`. Green; commit `feat: gates can play in order and carry their own game items`.

### Task F2: Game-day items for adult-2 and college-5

**Files:** `curriculum.json` (or the level files, following where gates live), `docs/CURRICULUM.md`.
- [ ] 8 new `situation` items per gate (ids `a2-game-01..08`, `c5-game-01..08`), BEST/OK/POOR with explanations, concepts from the glossary, at each level's knowledge; none duplicates a lesson item.
- [ ] Validator green; commit `content: game-only situations for the adult-2 and college-5 games`.

### Task F3: Kid ladder terms and thin basics

**Files:** kid level JSON (kid1..kid3), adult1 JSON, `C/content/ContentValidator.kt` (per-ladder prerequisite rule), `docs/CURRICULUM.md`; Test `ContentValidatorTest.kt`, `GlossaryQuizTest`.
- [ ] Test `prerequisiteMustBeGradedOnSameLadder` (fixture: concept on kid ladder whose prerequisite is graded only on adult → rejected). Run on real content: it should fail for motion/double-team/audible/disguise/two-point before content lands.
- [ ] Add ~2 graded kid items each for motion, double-team, audible, disguise, two-point (at or above each term's glossary level; NFL FLAG framing where flag differs, "your league may differ"); +2 graded items on each ladder for field, defense, snap, tackle, turnover, eligible-receiver where that ladder has one. Kid text: short sentences, "they".
- [ ] Green; commit `content: the kid ladder grades the terms the shared levels build on`.

### Task F4: Review page

- [ ] `python3 tools/review.py status` lists exactly the new/changed steps; export and build the page (`tools/review.py export` / `page`) into the scratchpad for the coordinator to publish. CURRICULUM.md counts updated. Commit if anything changed.

## PR G — branch `spec-honesty`

### Task G1: SPEC says what's built

**Files:** `docs/SPEC.md` (4.2, 4.3, 4.7, 6.2, 6.9, 14), plus this plan and the design doc under `docs/superpowers/`.
- [ ] Mark each v2 step type, the five unbuilt games, Level 0, stickers/badges/team board, lesson resume and checkpoint weighting as **Planned (not built)**; keep wording otherwise. No behaviour claims that PRs A-F change (those PRs edit their own sections).
- [ ] Commit `docs: the spec marks what's planned versus built`.
