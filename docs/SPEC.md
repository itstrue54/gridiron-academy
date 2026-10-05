# Gridiron Academy: spec

A learn-football app for kids (starting at about age 8) and adults. You progress by
passing lessons, a level test, and a level game. It starts with the field and ends with
NFL personnel packages, schemes and coverage disguises. See `docs/CURRICULUM.md` for the
level map.

## 1. Learning design
- **Path:** 8 levels; kid and adult tracks for levels 1-3, converging at level 4.
- **A level** is units of lessons, then a **level test**, then a **level game**.
  Lessons open in order (pass = 60%); the test opens when all lessons are passed
  (80% to pass); the game opens after the test (70% to pass). Passing both passes the level.
- **Placement:** adults take a one-quiz-per-level placement and start at the first level
  they miss (80% to pass each). Any level test can be taken to skip its lessons.
- **Missed items** come back once at the end of a lesson, and go into a spaced review
  queue (boxes at 1, 2, 4, 8, 16 days). A first-attempt score is what counts.
- **Streaks and XP** exist to build a habit; they never gate content.
- **Stars:** 95+ three stars, 80+ two, 60+ one.

## 2. Architecture
Pure Kotlin/JVM `:engine` (no `android.*`), content as JSON resources, UI to come in
`:app` (Compose). Dependency direction: `app -> engine`.

- `model/`: immutable `@Serializable` data classes: `Step` (sealed), `Diagram`, `Level`.
- `content/`: `ContentLoader` (reads JSON from any `ContentSource`) and `ContentValidator`.
- `engine/`: `Grader`, `LessonSession`, `SpeedRound`, `GameDay`, `LevelTest`, `Rng`.
- `progress/`: `Progress` (immutable), `Gating`, `Review`, `Placement`.

### Step types
`learn`, `multiple_choice`, `true_false`, `match`, `order`, `diagram_tap`,
`read_defense` (pre-snap picture, answer, post-snap reveal, optional `disguised`),
`situation` (down, distance, clock, score; one BEST, optional OK, POOR).

### Diagrams
One model for formations, route concepts, coverage shells, and rotations. Field
coordinates in yards (x across, y downfield of the LOS). Paths have a kind: pass, run,
motion, block, blitz, rotate, drop. Zones are shaded rectangles.

### Games
- **Speed round:** timed multiple-choice/true-false; a streak of 3 doubles points.
- **Read the defense:** series of `read_defense` steps.
- **Game day:** series of `situation` steps; scored in coach points (BEST 1, OK 0.5, POOR 0).
- **Diagram dash:** series of `diagram_tap` steps.

## 3. Rules the code follows
1. Randomness only from the seeded `Rng`; never `Math.random` or `System.nanoTime` in the engine.
2. Time is passed in (`nowMillis`, epoch day), never read from a clock.
3. All curriculum text lives in JSON, not Kotlin. `ContentValidator` must pass in CI.
4. Every graded step has an `explanation`.
5. Models are immutable; state transitions return new objects.

## 4. Content validation (enforced by `ContentTest`)
Unique ids; answer index in range; no duplicate choices; one BEST per situation; diagram
targets exist; routes reference real players; read-defense pre/reveal show the same
players; disguised reveals contain movement; every concept id is in the glossary; each
level's gate is playable; kid text limits.

## 5. Open items
App shell (Compose), speech (read-aloud), save format versioning, accounts and privacy
(especially for children; see COPPA and Google Play Families policy), monetization, art.
