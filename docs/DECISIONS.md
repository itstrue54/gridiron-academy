# Decisions

Short records of choices that are not obvious from the code. Newest last. SPEC 11.6
lists the ones still to be written.

## ADR-003: Kotlin Multiplatform core, with iOS targets declared early (2026-10-14)

**Context.** The apps ship on Android first but must reach iOS and a web coach
dashboard without a rewrite (SPEC 1.3, 11.3). The engine was a pure Kotlin/JVM module.

**Decision.** `:engine` became `:shared:core`, a KMP module with `jvm`, `android` and
`iosArm64`/`iosSimulatorArm64` targets. Everything except content reading lives in
`commonMain`.

**Why declare iOS now.** With only JVM-family targets (jvm + android), Kotlin does not
compile `commonMain` on its own, so `java.util.UUID` or `Math.round` in common code
compiles happily and breaks only when iOS is added months later. Declaring the iOS
targets turns on `compileCommonMainKotlinMetadata`, which rejects them today. The iOS
targets cannot link on Linux; `kotlin.native.ignoreDisabledTargets` keeps the build
quiet, and no Kotlin/Native toolchain is downloaded for the metadata compile.

**Consequences.** `jvmTest` and `check` both depend on the common compile and on
`checkDeterminism`, so the usual commands enforce both house rules.
Compose stays out of `core`. Content JSON is packaged once as Java resources and read
on jvm and Android by `ClasspathContentSource` in a shared `jvmShared` source set;
iOS needs its own reader at M5.

## ADR-007: Field diagrams stay to scale; crowding is solved with zoom (2026-10-14)

**Context.** A full-width formation on a 360dp phone is about 6dp per yard, so linemen
1.6 yards apart are about 10dp apart and a defensive lineman a yard off the ball
touches the man across from him. Marks already shrink to 18dp, which is not enough.

**Options.** (a) Keep one uniform scale and add pinch zoom. (b) Stretch the area
around the ball (a piecewise "box lens") so the line reads at 1x, at the cost of
true distances and angles near the ball.

**Decision.** (a), chosen by the product owner. Football is taught spatially
(DESIGN 1.1): depth, splits, leverage and alignment are the lesson, and a
lens would quietly misteach them. Zoom is pinch plus +/− buttons (DESIGN 7.10).

**Consequences.** Crowded diagrams look busy until zoomed; the buttons appear only
then. Grading of tap, `align` and `draw_route` steps uses the zoomed transform, so
answers are the same in yards at any zoom.

## ADR-008: Prime Time look for teens and adults (2026-10-15)

**Context.** The Adult skin ("Film Room") was a quieter version of the kid look. The
product owner asked for a redesign that makes the app enticing to teenagers and adults.

**Options.** Three directions were mocked up with Nano Banana (`docs/design/concepts/`):
A Prime Time (night-game broadcast), B Chalk Talk (coach's chalkboard), C Season Mode
(sports video game menu).

**Decision.** A mix of A and C, chosen by the product owner: Prime Time's always-dark
broadcast look with Season Mode's shield rank badges and XP bars (DESIGN 2.2). The
kid look is unchanged.

**Consequences.** Adults lose the light theme: Prime Time is a night game by design. A
"Daylight for adults" setting can return with profile settings if people ask. The
field diagrams keep their turf, so content and the renderer are untouched; contrast is
re-measured and tested for the new palette.

## ADR-009: Build the Coach app against a repository seam and a demo account (2026-10-16)

**Context.** M3's Coach app needs a backend (SPEC 8.2: Supabase or Firebase, still
open), accounts and verifiable parental consent (SPEC 5.4: vendor is a legal decision).
None of those are decided, but the dashboards, the planner and the assign flow are.

**Decision.** The coach screens read a `CoachRepository` (in a new `:shared:data`
module) that exposes one `CoachSnapshot` of synced events, and compute every number
with the shared engine (`CoachDashboard`). Until the backend exists the app runs on
`DemoCoachRepository`: a deterministic, fictional parent-who-coaches with a
high-school team, generated from a seed and the date. The app says it is a demo on
Players and More, and has no INTERNET permission.

**Consequences.** The backend choice only adds a second repository; screens and engine
don't change. Mastery in the coach app needs per-question attempt events, which the
learner app doesn't record yet: that is the next learner change, with a save
migration (SPEC 9). Prerequisite warm-ups wait for the concept graph.

## ADR-004: Supabase for the backend (2026-10-16)

**Context.** The Coach app needs accounts, linking, an event log readable by parents and
coaches, and team aggregates (SPEC 8). SPEC 8.2 recommended Supabase over Firebase.

**Decision.** Supabase, chosen by the product owner. Postgres row-level security carries
every access rule (SPEC 5.4, 8.5) in `backend/supabase/migrations/`, so a bug in an app
can't widen access. The rules are tested on PGlite, real Postgres in Node, against a
small stand-in for Supabase's `auth` schema (`backend/test/`, `npm test`), because the
local Supabase stack needs Docker.

**Consequences.** Team reports can be SQL views. The app clients use supabase-kt (KMP)
behind `CoachRepository`. Learner devices without an account (children) authenticate
with a token scoped to one learner (`learner_id` claim) minted at linking by an edge
function. Team membership has a status the coach can't set: the parent (or a teen) approves
through `set_membership`, which refuses a child without consent on file. The consent
method itself (VPC vendor) is still a legal decision.

## ADR-010: A prerequisite below the learner's level counts as known (2026-10-19)

**Context.** Assignment warm-ups (SPEC 7.4) add lessons for prerequisites a learner hasn't
mastered. "Mastered" is strict (several right first tries on two days), and the answer
log only began with save schema 2, so a Level 6 quarterback, or anyone whose progress
predates the log, showed as missing Level 1 to 3 basics and got warm-ups for them.

**Decision.** A prerequisite is known if it's mastered, or if it's first taught at a level
below the learner's current level. Warm-ups cover only basics at or above where the
learner is, capped at four lessons, with the rest counted in the preview.

**Consequences.** Experienced players get no busywork; a beginner assigned an advanced
pack still gets the nearest basics first, and the coach sees how many more exist and the
"more than two levels above" warning. If passing a level ever stops implying its basics
(test-outs are already gated by an 80% Checkpoint), this rule would need revisiting.

## ADR-011: The Playbook map lays out by depth, one tab per side (2026-10-20)

**Context.** DESIGN 9.14 asks for the concept graph as a vertical skill tree. The graph
has 152 concepts and 220 links, and some depths hold 17 concepts; a general graph layout
on a phone is a tangle.

**Decision.** Each glossary term carries a `side` (content). A tab draws only its side:
a concept's row is one below its deepest same-side prerequisite, a row holds at most four
nodes (wider depths wrap), and order within a depth follows the prerequisites' positions.
Cross-side prerequisites are named in the concept's sheet instead of drawn. Lines are
faint until a node is tapped. The validator forbids a prerequisite that another direct
prerequisite already implies, so no line is drawn twice.

**Consequences.** The layout is pure, deterministic and tested in core; it needs no graph
library. Long hub edges (Offense to everything) still cross rows, which is why lines stay
faint and List mode exists. A side changes only in glossary.json.

