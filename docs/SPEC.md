# Gridiron Academy: product and technical spec

**Status:** draft v2 (2026-10-14). Replaces the 60-line v1 spec.
**Companion docs:** `docs/CURRICULUM.md` (level-by-level content map),
`docs/DESIGN.md` (visual design system, cited as DESIGN §n), `docs/DECISIONS.md`
(ADRs, to be created).

This file is the source of truth. Code and content cite it by section number; change
the section in the same commit as the behaviour it describes. Section numbers that
DESIGN.md already cites (3.3, 5.2, 5.3, 6.4, 11.2, 13.4) are deliberately kept stable.

Items marked **[review]** are football or legal details to be verified by a qualified
person before they reach a learner (AGENTS rule 7). Items marked **[open]** are
product decisions not yet made; they are collected in §15.

---

## 1. Product

### 1.1 Vision
A Duolingo-style course that takes anyone from "what is a touchdown?" to reading an
NFL coverage disguise before the snap. Short lessons, games and tests on an animated
field, a clear ladder of levels, and a separate **Coach & Parent app** that lets the
adults in a learner's life see progress and assign the exact offense and defense
their team plays.

### 1.2 Two apps, one platform

| | **Gridiron Academy** (learner app) | **Gridiron Academy Coach** (coach & parent app) |
|---|---|---|
| Who | Learners aged ~4 to adult | Parents, guardians, coaches, teachers |
| Does | Lessons, games, tests, review, assignments, team playbook study | Link learners, dashboards, assign scheme packs, build a team playbook, reports |
| Works offline | Yes, fully | Read-only cache offline; assigning needs a connection |
| Needs an account | No (local profiles). Only to link to a coach or parent | Yes (adult account) |
| Store category | Education; Play **Families** programme for the kid audience | Education / Sports; general audience (adults only, 18+) |

Separate apps (not one app with a mode) because: the learner app targets children
and must meet Families policy, the coach app is adult-only and handles accounts and
team data; separate listings keep each simpler to review and keep adult features
physically off a child's device. Both are built from the same shared Kotlin code
(§11). See ADR-001 in §11.6.

### 1.3 Goals
1. Teach real football correctly at every age. Accuracy beats volume.
2. Go deep: every common offensive and defensive scheme, personnel package,
   coverage, pressure and disguise at NFL level (§3.2).
3. Make the daily habit fun and short (3-10 minute sessions).
4. Give coaches and parents a reason to put the app in a player's hands: assign
   "learn *our* defense" and see who knows it.
5. Ship Android first; keep every non-UI line of code multiplatform so iOS and a
   web coach dashboard are a port, not a rewrite.

### 1.4 Non-goals
- Not a video/film platform (no game-film upload in v1; §15).
- Not a fantasy, betting, or real-team stats app. No real teams, players or logos.
- No social network: no free-text chat with children, no public profiles.
- Not a physical training/drills app (on-field conditioning, tackling technique).
  Safety content is limited to rules and sportsmanship.

### 1.5 Principles
1. **The field is the canvas** (DESIGN §1.1). Show it on a field before saying it.
2. **A wrong answer is a teaching moment.** No hearts, no lives, no lockouts, no red
   (DESIGN §1.3). Every graded item explains why.
3. **Habit, not guilt.** Streaks and XP motivate but never gate content; no streak
   shame, no fake urgency, no ads to kids.
4. **Deterministic engine.** Same content + same seed + same answers = same result,
   so tests, sync replay and bug reports are exact.
5. **Content is data.** Lessons, schemes, glossary, voice lines and diagrams are JSON;
   adding a scheme never needs a recompile or an app release (§3.6).
6. **Private by default.** Nothing about a child leaves the device until a parent has
   given verifiable consent in the Coach app (§5.4).

### 1.6 Success measures
| Measure | Target (6 months after public launch) |
|---|---|
| D7 / D30 learner retention | 30% / 15% |
| Lesson completion (started -> finished) | 85% |
| Level test first-attempt pass rate | 60-80% per level (outside = content too easy/hard) |
| Items with p-value < 0.3 or > 0.97 | < 5% of graded items (content QA, §12) |
| Assignment completion by due date | 65% |
| Coach weekly active / linked coaches | 50% |

---

## 2. Audiences

| Persona | Age | Starts at | Wants | Key constraints |
|---|---|---|---|---|
| **Little one** ("Mia, 5") | 4-7 | Level 0 | Play, sounds, stickers; usually with a parent nearby | Cannot read; tiny attention span; fat-finger taps |
| **Young player** ("Leo, 9") | 8-12 | Kid L1 | Understand flag/youth football; feel smart at practice | Reads slowly; read-aloud on; COPPA applies |
| **Teen player** ("Jordan, 15") | 13-17 | Placement | Learn the high-school playbook, earn the coach's trust | Own account allowed (13+); team-linked |
| **New fan** ("Priya, 34") | adult | Adult L1 | Follow a game with friends without asking | Busy; wants fast lessons; skip what they know |
| **Serious fan** ("Dan, 41") | adult | Placement -> L5+ | Understand what TV analysts mean; disguises, schemes | Wants depth and correct terminology |
| **Parent** ("Sam") | adult | Coach app | See that their child is learning; set time limits | Not a football expert; needs plain language |
| **Youth/HS coach** ("Coach Ray") | adult | Coach app | Teach 25-60 players the team's offense and defense | Little time; wants per-player readiness at a glance |

---

## 3. Curriculum and content

### 3.1 The ladder
Nine levels (0-8). Kid and adult tracks run separately for levels 1-3 and converge
at level 4 (decision 2026-10-05). **Level 0 is new in v2** and sits below Kid L1.

| # | Kid track | Adult track | Approx. audience | Gate game |
|---|---|---|---|---|
| 0 | **Little Huddle** | (none) | 4-7, pre-readers | Picture Play (no test, see §4.4) |
| 1 | Pee-wee | Fan Starter | 8 / new fans | Speed round |
| 2 | Rookie | Season Ticket | 9-10 / casual fans | Speed round / Game day |
| 3 | Junior Varsity | Armchair Coach | 11-12 / fans | Diagram dash |
| 4 | Varsity (shared) | | 13+ / both | Read the defense |
| 5 | College (shared) | | | Game day |
| 6 | Pro (shared) | | | Read the defense |
| 7 | Coordinator (shared) | | | Read the defense |
| 8 | Architect (shared) | | | Game day |

A kid who finishes Kid L3 continues into the shared levels; an adult can place
straight into any level (§4.5). `docs/CURRICULUM.md` holds the topic list per level;
this spec does not duplicate it, except Level 0, which is new:

**Level 0: Little Huddle** (audio-first, no reading required)
- Units: *The Ball* (shape, it bounces funny, you can throw, run, kick it);
  *Two Teams* (one has the ball, one tries to stop them; colours as shirt colours);
  *Go That Way!* (each team runs toward an end zone); *Touchdown!* (ball in the end
  zone = 6, count to 6 on fingers); *Run, Throw, Catch, Kick*; *Flags and Tackles*
  (flag pulling as the "tackle" for little kids); *The Referee* (whistle, touchdown
  signal, "stop" signal); *Good Sport* (take turns, high-five, helmet and mouthguard
  safety).
- Every instruction is spoken (recorded voice, not TTS; §6.9), every answer is a
  picture, there is no timer, no text-only screen, and a session is 3-5 minutes.
- Includes **co-play cards** for a grown-up: "Ask your child to show you the
  touchdown signal."

### 3.2 Scheme catalogue (what "all schemes" means)
Every concept below gets a stable **concept id** (dot-path, e.g.
`def.cov.cover3.buzz`), a glossary entry, a home level, prerequisites (§3.2.7) and at
least one diagram. Coaches assign from this catalogue (§7.4). Terminology varies
between staffs; content uses the most common name, lists aliases, and says when a
term is staff-specific **[review]**.

#### 3.2.1 Offense: structure
- **Personnel:** 00, 01, 02, 10, 11, 12, 13, 20, 21, 22, 23, jumbo (6 OL), heavy goal
  line, wildcat.
- **Formations:** I, offset I, Power I, singleback/Ace, pro (split backs), shotgun,
  pistol, empty, twins, trips, bunch, stack, tight/nub, wing, wing-T, double wing,
  diamond, unbalanced, victory/kneel.
- **Strength and splits:** formation strength, passing strength, tight vs wide
  splits, nasty splits, compressed sets.
- **Motion and shifts:** jet, orbit, return, across/zip, fly, short motion, shift,
  trade, motion-at-snap; rules (one in motion, not toward the LOS at the snap).
- **Tempo:** huddle, no-huddle, hurry-up, NASCAR/sugar huddle, freeze/check-with-me.

#### 3.2.2 Offense: run game
- **Zone family:** inside zone, outside/wide zone (stretch), split zone, zone
  with insert/arc blocks.
- **Gap family:** power (G, O), counter (GT, GH, trey), trap (short, long), iso/lead,
  duo, dart, pin-and-pull, buck sweep, toss/pitch, sweep, QB power, QB counter,
  draw (lead, delay, QB), sneak, wedge.
- **Option family:** zone read, inverted veer/power read, midline, veer, triple
  option, speed option, load option, RPO family (bubble, glance, stick, now, slant).
- **Blocking concepts:** combo/double-team, reach, down, kick-out, wrap, pull,
  base, cut-off, climbing to the second level, read vs block (option keys).

#### 3.2.3 Offense: pass game
- **Route tree** 0-9 and naming conventions (numbering differs by system **[review]**),
  plus option, whip, wheel, seam, sail, dig, deep over, choice, angle/Texas.
- **Quick game:** slant-flat, stick, snag/spot, speed out, hitch-seam, all curl, fade,
  double slants, quick screen.
- **Dropback concepts:** smash, flood/sail, levels, mesh, shallow cross/drive, Y-cross,
  dagger, mills, four verticals, curl-flat, spacing, Hank, Y-option, drift, yankee.
- **Play-action:** boot/keeper, naked, flood off boot, deep shot (yankee/post-wheel),
  leak, half-roll.
- **Screens:** RB slip, swing, bubble, tunnel, middle, jailbreak, TE delay.
- **Coverage beaters:** high-low, horizontal stretch, vertical stretch, triangle
  reads, man beaters (rub/pick legality, mesh, option routes, stacks/bunch),
  Cover 2 hole shot, Cover 3 seam and curl-flat stress, Cover 4 attacks.
- **Protections:** slide, half-slide, man (BOB), 6- and 7-man, max, empty, sight
  adjust, hot throw, MIKE identification, chips, play-action protection.
- **QB process:** progressions, pre-snap and post-snap reads, alerts, kills/checks.

#### 3.2.4 Defense: structure
- **Fronts:** even (4-3 Over, Under, Wide-9), odd (3-4 Okie, 5-2, Tite/Mint),
  Bear/46, Eagle, 3-3-5 stack, 4-4, 5-3, goal line 6-2-3 and 5-3-3.
- **Packages:** base 4-3/3-4, nickel 4-2-5, big nickel, dime 4-1-6, quarter
  3-2-6, dollar 3-1-7.
- **Alignment language:** gap letters A-D, techniques 0-9 (with shade conventions,
  which differ by staff **[review]**), 1-gap vs 2-gap, Mike/Will/Sam/Jack, nickel/star,
  money/robber.
- **Run fits:** gap control, force/contain, spill vs box, alley, cutback, scrape
  exchange, gap integrity vs option (dive/QB/pitch players).
- **Line games:** T-E and E-T twists, loops, slants, pinch, NT wrap.

#### 3.2.5 Defense: coverage and pressure
- **Man:** press, off, trail, bail, inside/outside leverage, banjo, bracket, combo.
- **Coverages:** Cover 0; Cover 1 (free, robber, lurk, rat, hole); Cover 2 (zone);
  Tampa 2; 2-man; Cover 3 (sky, buzz, cloud, mable, seam-match); Cover 4 (quarters,
  MOD, MEG, Palms/2-read, solo, stubbie); Cover 6 (quarter-quarter-half); Cover 9 and
  Cover 7 variants (names differ by staff **[review]**); pattern matching (rip/liz
  match, 3-deep match).
- **Pressures:** 5-man fire zone (3-under 3-deep), 6-man, Cover 0 blitz, double
  A-gap, cross dog, overload, edge, corner and nickel blitz, delayed blitz,
  simulated pressure/creeper (4 rushers, one non-traditional).

#### 3.2.6 Disguise and counter-disguise (Level 7 core)
- **Shell disguises:** two-high rotating to one-high (sky, buzz, cloud rotations),
  one-high spinning to two-high, late rotation at the snap, post-motion rotation.
- **Pressure disguises:** bluff blitz, sugar (mugged A-gaps), creeper, show-and-drop,
  drop-out of a walked-up look, "show 0, play 2."
- **Coverage disguises:** trap coverage (corner sinks from a flat look), fake man
  (press then bail), fake zone, robber from a two-high look, bracket in a single-high
  shell, cornerback leverage tells.
- **Offensive answers:** motion to identify man/zone, formation tells, tempo to
  prevent rotation, check-with-me, alerts, hot routes, sight adjusts, protection
  slides to the pressure, bunch/stack release vs man.
- **Defensive answers to answers:** check systems (red/green, "check-with-me" vs
  tempo), match rules, communication calls (motion "Ram/Lion" style adjustments,
  names vary **[review]**).
- Every disguise item is a `read_defense` step with a pre-snap picture and a reveal;
  the explanation says which clue pointed at the answer and that **a tell is a clue,
  not a guarantee** (CURRICULUM content rule 4).

#### 3.2.7 Systems, special teams, strategy
- **Offensive systems:** West Coast, Erhardt-Perkins, Air Coryell, Air Raid,
  run-and-shoot, spread/RPO, Shanahan-style wide zone and play-action, pistol,
  spread option, flexbone, Wing-T, double wing, single wing (history).
- **Defensive systems:** Fangio/Vic two-high, pattern-match quarters, Seattle Cover 3,
  Tampa 2, 46, Wide-9, 3-3-5 odd stack, 5-2 youth defences.
- **Special teams:** kickoff (with the current NFL kickoff format, stamped by season
  **[review]** each offseason), onside, punt (spread, shield, rugby), punt return
  (wall, middle), field goal/PAT protection, fakes, blocks, coverage lanes, touchbacks,
  fair catch.
- **Strategy:** 4th-down decisions, two-point decisions, clock management, two-minute
  and four-minute offense, red zone, goal line, 3rd-down, EPA, success rate, win
  probability, roster/cap basics (Architect only).
- **Rule variants:** NFL, NCAA, high school (NFHS), youth tackle, and flag
  (5-on-5 style: no-run zones, rusher depth, pass clock, no contact). Variant rules
  differ by league; content names its variant and the league-agnostic default is
  stated **[review]**.

**Concept graph.** Every concept lists `requires` (other concept ids). The graph is
validated acyclic in CI. It drives prerequisite primers for assignments (§7.4),
the mastery map (§4.6) and placement (§4.5).

### 3.3 Content format, validation, load failure
- Content is JSON under `shared/core/src/commonMain/resources/content`, packaged as
  Java resources into the JVM jar and the Android library, so there is one copy.
  `commonMain` only defines the `ContentSource` interface; `ClasspathContentSource` in
  the `jvmShared` source set (jvm + android) reads the packaged files. iOS gets its own
  reader at M5 (§11.3).
- Files: `curriculum.json` (levels and gates), one file per unit, `glossary.json`,
  `concepts.json` (§3.2 catalogue + graph), `packs.json` (scheme packs, §7.4),
  `voice/<locale>.json` (feedback copy, DESIGN §11), `audio/` (Level 0 recordings).
- Each file has `schemaVersion` and the bundle has a `contentVersion` (monotonic
  integer) and a `ruleSeason` (e.g. `2026`).
- **Validation** (`ContentValidator`, enforced in CI by `ContentTest`): everything in
  v1 §4 (unique ids; answers in range; no duplicate choices; one BEST per situation;
  diagram targets exist; routes reference real players; read-defense pre/reveal show
  the same players; disguised reveals contain movement; every concept is in the
  glossary; every gate is playable; kid text limits) plus: concept graph acyclic and
  complete; every pack resolves; Level 0 steps have audio for every prompt and choice
  and no text-only choice; every `[review]`-flagged step is excluded from release
  builds unless signed off (§13.3).
- **Load failure** never crashes the app: a bad or missing file shows a kind error
  with **Try again** (DESIGN §9.11), falls back to the last good bundle (bundled or
  previously downloaded), and writes a local diagnostic. Release content cannot fail
  validation, because the build fails first.

### 3.4 Step ids are forever
A step's `id` is permanent once shipped. Editing text keeps the id; changing what is
being asked (the answer) creates a new id and **tombstones** the old one
(`retired: true`), so historical attempts, mastery and synced events stay valid.

### 3.5 Rule era and variants
Any step that depends on a rule that changes (kickoff format, overtime, catch rule,
targeting) carries `ruleSeason`. Each offseason a check job lists such steps for
re-review. Where variants differ, the step's `variant` field (`nfl`, `ncaa`, `hs`,
`youth`, `flag`, or `any`) is shown as a small chip in the UI.

### 3.6 Content delivery
- A full content bundle ships inside the app (works offline, first run).
- **Content updates over the air:** the app checks a static CDN manifest at most once
  a day; a newer `contentVersion` compatible with the app's `minContentSchema` is
  downloaded as a signed zip (Ed25519 signature verified on device), validated with
  the same `ContentValidator`, and swapped in atomically. Failure keeps the old bundle.
- New levels, schemes, packs and rule fixes ship this way, with no app release.

### 3.7 Authoring workflow
JSON is edited directly (the old Python generator is retired). A dev-only **Content
preview** screen in the learner app (debug builds) hot-loads a unit file from device
storage and renders every step, so authors see diagrams as learners will. Football
review sign-off is recorded per step in `review.json` (reviewer, date, ruleSeason).

---

## 4. Learning design

### 4.1 Structure
`Level -> Unit -> Lesson ("drill") -> Step`. A lesson is 6-12 steps (Level 0: 4-6),
about 3-5 minutes. Units are 3-6 lessons. A level ends with a **Checkpoint** (level
test) and a **Big Game** (level game), as in v1.

### 4.2 Step types
Existing (v1): `learn`, `multiple_choice`, `true_false`, `match`, `order`,
`diagram_tap`, `read_defense` (pre-snap, answer, reveal, optional `disguised`),
`situation` (down, distance, clock, score; BEST/OK/POOR).

New in v2:

| Type | What the learner does | Used for |
|---|---|---|
| `picture_choice` | Hears a prompt, taps one of 2-4 pictures | Level 0, kid levels |
| `drag_to` | Drags (or taps then taps target) an item to a spot on the field | Level 0 "ball to the end zone", alignment drills |
| `listen_tap` | Taps things on a scene; each speaks its name (exploration, ungraded) | Level 0 |
| `draw_route` | Draws a path from a player; graded against a target route with tolerance | Route tree, assignments |
| `align` | Places one or more players at the right alignment (technique, depth, leverage) | Fronts, techniques, team playbook |
| `film` | Watches an animated play (diagram + timed paths), then answers | Run/pass concepts, penalties |
| `spot_foul` | Watches a play, taps the player who committed a foul and names it | Rules, refereeing |
| `call_it` | Sees a defense (and situation), picks the best play from 2-4 | Offense vs defense, Game Day |
| `my_job` | Given a play and a position, answers "what's my assignment?" (tap, draw or choose) | Team playbook (§7.5), position packs |

All graded types reduce to `Grader` results of `correct | partial(pct) | wrong`;
`draw_route` and `align` use a deterministic tolerance check (distance in yards,
direction of break within an angle) defined as named constants in `GradingRules`
in the engine, unit-tested.

### 4.3 Games (activities)
| Game | Mechanic | Levels |
|---|---|---|
| **Picture Play** | 6 untimed `picture_choice`/`drag_to` rounds, sticker reward | 0 |
| **Speed round** | Timed MC/TF, streak of 3 doubles points; **Untimed** option keeps scoring without a clock (DESIGN §12) | 1-3, review |
| **Diagram dash** | Run of `diagram_tap`/`align` | 3+ |
| **Read the defense** | Run of `read_defense`, disguise flag at L7 | 4, 6, 7 |
| **Game day** | A simulated game of `situation`/`call_it` decisions; coach points (BEST 1, OK 0.5, POOR 0) | 2, 5, 8 |
| **Route runner** | Run of `draw_route` against shown coverage | 3+ |
| **Ref school** | Run of `spot_foul`/`film` | 2+ |
| **Playbook quiz** | Generated from the team playbook (§7.5); `my_job` per position | Any, team-linked |
| **Daily challenge** | One seeded mixed set per day (seed = epoch day + profile), same for everyone at a level so friends/teams can compare | All |

Game sets are drawn with the injected `Rng` seeded from (profile, item set, attempt
number), so a replay of the same attempt is identical (principle 1.5.4).

### 4.4 Gates
- Lessons open in order; pass = 60% first-attempt score.
- Checkpoint (level test) opens when every lesson is passed; 12 questions; 80% to
  pass. Drawn from the level's graded steps, weighted toward concepts with low
  mastery, never repeating within an attempt.
- Big Game opens after the checkpoint; 70% to pass.
- Passing both passes the level. Stars per DESIGN (95/80/60).
- **Level 0 has no test.** Finishing every lesson and one Picture Play passes it, so
  no pre-reader ever "fails" a test. A kid can also skip Level 0 entirely.
- **Assignment unlock:** a coach assignment can open a lesson above the learner's
  level (§7.4); it does not pass the level.

### 4.5 Placement
- Kid profiles start at Level 0 if the age band is 4-7, else Kid L1. An optional
  "I know some football" quiz is offered from age 10.
- Adults/teens: one 6-question quiz per level, ascending, stop at the first level
  under 80%; earlier levels are marked passed via `Placement.apply` (existing).
- Any level test can be taken to **test out** (existing `Gating.testOut`); the Big
  Game is still required.

### 4.6 Mastery model (new)
Each concept id has a per-profile mastery record derived from attempts:
`state in {NEW, LEARNING, PRACTICED, MASTERED, RUSTY}` and a score 0-100.

- Score: exponentially weighted first-attempt correctness over the concept's last
  10 graded attempts (weight 0.8^age), partial credit counts proportionally.
- `MASTERED`: score >= 80, at least 3 correct first attempts, on at least 2 distinct
  days, and the last attempt correct.
- `RUSTY`: was MASTERED and either 30+ days without an attempt or a review miss.
- All thresholds are named constants in `MasteryRules`; time is passed in as epoch
  day. Pure function `Mastery.of(events, conceptId, today)`, deterministic.

Mastery drives: the coach dashboard (§7.3), the checkpoint weighting, daily practice
selection, and the learner's **Playbook map** (the concept graph rendered as a
skill tree that fills in).

### 4.7 Motivation
- **XP** per lesson/game (existing), **streaks** (existing, with one free "rest day"
  per week so a missed Sunday doesn't break a kid's streak), **stars**, **stickers**
  (Level 0 and kid track: one per lesson, collected in a sticker book),
  **badges** for milestones (first level, first disguise read, 30-day streak).
- **Team board** (team-linked only, opt-in by the coach, under-13s shown by
  nickname only): weekly XP and assignment completion. No global public leaderboard.
- No hearts, lives, energy, paid continues, or loot boxes.

---

## 5. Profiles, age, privacy, purchases

### 5.1 Profiles
The learner app holds up to 6 local profiles (a family tablet). A profile is a
nickname, an avatar (one of 8 helmets, recolourable), an age band, a track and
local progress. No real name, photo, email, location or birthday is ever collected
on the learner device.

### 5.2 Age screen
On profile creation: "How old is the player?" with age bands as big buttons
(4-5, 6-7, 8-9, 10-12, 13-17, 18+), neutral, no default, no hint about which unlocks
what (Play Families neutral age screen). The band sets track, starting level, skin and
privacy rules. Changing the band later requires the grown-up gate (DESIGN §9.9).

### 5.3 Purchases [open]
Proposal (to be decided, §15):
- Learner app: levels 0-4 free forever. Levels 5-8 and team features via a
  **Family** or **Team** plan bought **only in the Coach app** by an adult, never
  inside the kid experience. The learner app shows "Ask a grown-up" with no price.
- Coach app: Parent tier free (up to 4 linked learners); Team tier paid per season
  (roster up to 60, playbook builder, reports); Club tier for leagues.
- No ads anywhere. No in-app purchases presented to under-13 profiles.

### 5.4 Privacy and children's data
- **Local-first.** Unlinked profiles never send personal data. The only network calls
  are the content manifest (§3.6) and, if enabled, anonymous crash reports without
  device identifiers. Analytics follow §12.
- **COPPA / GDPR-K / UK Age Appropriate Design Code.** A profile under 13 (and under
  16 in EU countries that set 16) can be linked only after **verifiable parental
  consent (VPC)** collected in the Coach app by the parent. The VPC method and vendor
  are a legal decision **[review][open]**. A coach can never link a child directly:
  the coach invites, the parent approves.
- **Data collected once linked:** nickname, avatar, age band, attempt events (§8.1),
  assignment state. Nothing else. Stored in a single chosen region; encrypted in
  transit and at rest.
- **Parent controls:** see everything the coach sees; revoke a coach; export all data
  (JSON); delete the child's cloud data (hard delete within 30 days, removed from
  backups within 90). Unlinking returns the learner app to local-only.
- **Retention:** inactive linked learner data deleted after 24 months of inactivity,
  with an email warning to the parent at 23 months.
- **Teens 13-17** may create their own account (email or platform sign-in) to join a
  team via code; the team sees only team-scoped data.
- Play **Families** programme: only Families-certified SDKs in the learner app; no
  advertising ID permission; privacy policy linked in-app and on the listing.

### 5.5 Safety
- No free-text from learner to anyone. Learners can send only preset reactions
  ("Got it!", "Need help", thumbs up) to a linked coach or parent.
- Coach -> learner: assignment notes (max 200 characters, a profanity filter, visible
  to the parent too) and preset **kudos** stickers. No images, links or contact details
  (validated server-side; URLs and phone/email patterns rejected).
- Coaches must accept a code of conduct; team owners can remove assistants; parents
  can report a coach (goes to a moderation queue).

---

## 6. Learner app

### 6.1 Home: the drive map
DESIGN §9.1: Level 0 sits **behind the goal line** as a "practice field"
pad for kid profiles 4-7 and is hidden for adults. An **Assignments** strip appears
above the primary action when a coach or parent has assigned something (§6.5).

Primary action priority: (1) an overdue assignment, (2) today's practice if cards are
due, (3) Continue.

### 6.2 Lesson player
DESIGN §9.3. New step types get their own composables (§4.2). The player is a
state machine in the engine (`LessonSession`), the UI only renders state and sends
answers. A lesson can be quit and resumed; partial state is saved after every step.

### 6.3 Games
DESIGN §9.4-9.6 for existing games; the new games reuse the same HUD components.
Game Day uses fictional teams (`TeamMark`).

### 6.4 Daily practice and spaced review
- Missed items enter a Leitner queue (boxes 1, 2, 4, 8, 16 days; existing `Review`).
- **Today's practice** (5-10 items) = due review cards, then RUSTY concepts, then
  LEARNING concepts from the current level, then assignment items; capped by the
  profile's daily limit. Selection is deterministic from (profile, epoch day).
- Practice counts toward the streak.

### 6.5 Assignments inbox
- Shows each active assignment: title, from (coach/parent display name), due date,
  progress ring, the note.
- Opening an assignment shows its plan (§7.4): prerequisite primers, lessons, the
  assigned games, and the final **assignment check** (a short test, default 80%).
- Assigned lessons above the learner's level are unlocked with an "Assigned by
  Coach Ray" badge.
- Completing sends a sync event; the coach sees it on next sync.

### 6.6 Team playbook (team-linked learners)
A read-only gallery of the team's plays (§7.5), filtered to the learner's
position(s). Each play: the diagram, the learner's job highlighted, an **animate**
button, and **Quiz me** (Playbook quiz, §4.3). Plays never leave the team's scope and
are removed from the device if the learner leaves the team.

### 6.7 Glossary and Playbook map
DESIGN §9.10, plus the **Playbook map**: the concept graph as a skill tree, coloured
by mastery state (with icons, not colour alone), searchable, each node opening its
glossary entry and a "Quiz me".

### 6.8 Offline and multi-device
Everything works offline. Unlinked progress lives on one device (with manual
export/import backup, DESIGN §9.9). Linked progress syncs across devices through the
event log (§8).

### 6.9 Level 0 mode specifics
- Kid skin with 64dp targets, no text on answer pictures, prompts spoken
  automatically and on tap of the big speaker.
- **Recorded voice** for every prompt, choice name and feedback line (`audio/` in
  content, ~150 short clips per locale, Opus, mono, 24 kbps). TTS is a fallback only.
- No timers, no stars (stickers instead), no scores shown; every answer gets a
  positive "Let's try that again together" path to the right answer.
- Session ends on its own after the configured limit with a goodbye from Clip.
- Grown-up co-play cards (§3.1) shown after each lesson, dismissible.

### 6.10 Settings and grown-up zone
DESIGN §9.9, plus **Link to a coach or parent** (scan a QR or type an 8-character
code shown in the Coach app), **Linked adults** (list, and for 13+ the option to
leave a team) and content-update status. The privacy statement reads "nothing
leaves this device unless a parent links it" (DESIGN §9.9).

---

## 7. Coach & Parent app

### 7.1 Roles
| Role | Can |
|---|---|
| **Parent/guardian** | Create managed kid learners; give VPC; link devices; view their learners; assign; set time limits; approve or revoke coaches; export/delete data |
| **Coach (team owner)** | Create a team; invite learners (needs parent approval for under-13s); view team learners; assign to team, groups or individuals; build the team playbook; add assistants |
| **Assistant coach** | Same as owner minus billing, deleting the team and removing coaches; can be limited to groups (e.g. "Defense") |
| **Teacher** (later) | Like coach, class terminology [open] |

One adult account can hold several roles (a parent who coaches).

### 7.2 Linking flow
1. Adult signs in (Google, Apple later, or email + password with verification).
2. **Parent path:** "Add your child" -> nickname, age band -> VPC (if under 13) ->
   a link code/QR. On the kid's device: grown-up gate -> Link -> scan. The local
   profile's history uploads as events (with the parent's consent shown on the
   consent screen).
3. **Coach path:** create team (name, sport variant: flag/youth tackle/HS/college/adult
   fan, age group, season) -> team code. Teens/adults join with the code in their own
   account. For under-13s the coach sends an invite to the parent (email or share link
   from the coach); the parent approves in their Coach app, which links the child.
4. Links are revocable from either side (except a child can't unlink themselves from a
   parent).

### 7.3 Dashboards
- **Learner card:** current level, streak, minutes this week, assignments (on track /
  behind / done), mastery map summary, the 3 weakest concepts with "assign practice"
  one-tap, recent activity timeline.
- **Team view:** roster table (sortable): level, last active, assignment completion,
  readiness for each assigned pack (% of pack concepts MASTERED). A **heat map** of
  learners x concepts (rows and columns orderable; icons + values, not colour only).
- **Pack readiness:** for an assigned scheme, which players know it, which concepts
  the team as a whole misses most (item-level miss rates), with the actual items
  so a coach can teach them at practice.
- **Position view:** filter by position group (QB, OL, WR, DL, LB, DB, ST).
- Parents see their child's card only. All numbers are derived from synced events
  with the shared `Mastery` code (§4.6), so learner app and coach app always agree.

### 7.4 Assignments and scheme packs
A **scheme pack** is a curated, versioned bundle defined in `packs.json`:
`{id, title, side: offense|defense|special|rules, level, concepts[], lessons[],
games[], check: {questions, passPct}}`. Examples:
- *Youth 5-2 defense with Cover 3*, *4-3 Over + Cover 2/Tampa 2*, *3-4 with fire-zone
  pressures*, *Quarters (MOD/Palms)*, *Disguise: two-high rotations*.
- *Wing-T base (buck sweep, trap, waggle)*, *Inside/outside zone with boot action*,
  *Air Raid core (mesh, Y-cross, four verticals, stick)*, *RPO starter*,
  *Flag 5v5 offense basics*.

**Creating an assignment:**
1. Choose what: one or more packs, or individual concepts/lessons from the catalogue
   (search + browse by side and level), or plays from the team playbook.
2. Choose who: team, group (e.g. "Linebackers"), or individuals.
3. Options: due date, required check score (default 80%), "include prerequisites"
   (default on), note (200 chars).
4. Preview the **plan** per learner: the engine computes, from the concept graph and
   each learner's mastery, the ordered list of prerequisite primers (only missing
   ones), lessons, games and the check. A learner who already has MASTERED concepts
   gets a shorter plan.

**Rules:** an assignment can unlock lessons above a learner's level but shows a
warning when more than two levels above (e.g. a 9-year-old assigned Level 7
disguises). Kid-track wording is used for kid profiles where a kid variant exists; a
pack lists any concept with no kid-appropriate version, and the coach sees that
before assigning. Assignments are immutable once sent (edits create a new version;
progress carries over by concept).

**Recurring assignments:** "10 minutes of practice, Mon/Wed/Fri" style goals.

### 7.5 Team playbook builder
- A play editor on the same `Diagram` model: start from a formation template (or a
  defensive front + coverage template), drag players, draw paths (pass, run, block,
  motion, blitz, drop, rotate), name the play, tag the concepts it uses (e.g. it is
  `off.run.counter.gt`), set each position's **job** text (200 chars).
- **Auto-generated quiz items** per play, deterministic and reviewable before
  publishing: "Where do you line up?" (`align`), "What's your route?" (`draw_route`),
  "Who do you block?" (`diagram_tap`), "What's your job?" (`my_job`), "Name this play
  from the picture" (`picture_choice`/MC). The coach can edit or delete any generated
  item.
- Playbook validated with the same `ContentValidator` rules as shipped content.
- Plays are private to the team, versioned, and can be shared to another team the
  same coach owns. Import/export as JSON. No uploading of images or video in v1.
- Tablet-first layout; works on phones in landscape.

### 7.6 Reports
Weekly email digest (opt-in) for parents and coaches: minutes, lessons, assignment
status, top misses. In-app printable/shareable PDF team report (generated on device).

### 7.7 Notifications
- Learner app: local reminders only (time chosen by the grown-up), plus "new
  assignment" pushes for linked profiles, worded neutrally, never guilt-based. Quiet
  hours default 20:00-08:00 for kid profiles.
- Coach app: assignment completed, learner behind on an assignment, parent approved a
  link, daily digest (configurable).
- Push via FCM on Android; payloads carry ids only, never names or content.

### 7.8 Coach app on other surfaces
A web dashboard (same shared code via Compose for Web/Wasm or a thin web client, §11.3)
is planned for M5, because coaches plan on laptops.

---

## 8. Sync and backend

### 8.1 Event log
All learner state is a fold over an append-only event log, computed by the shared
engine. Events:

```
AttemptEvent(learnerId, deviceId, seq, stepId, contentVersion, context,
             result: correct|partial|wrong, pct, answer, msSpent, epochDay, at)
SessionEvent(... type: lesson_start|lesson_end|game_end|test_end|placement, ...)
SettingEvent(... key, value)        // track, age band, skin, daily limit
AssignmentEvent(... assignmentId, type: opened|completed|check_result)
```

- Event identity is `(deviceId, seq)`: unique without randomness. `deviceId` is
  generated once by the platform layer (not the engine) at install.
- `Progress`, review queue and mastery are derived by `Projection.fold(events)`;
  cached snapshots are an optimisation, never the source of truth.
- Merging events from two devices is a set union; the fold orders by
  `(epochDay, at, deviceId, seq)`, so the result is deterministic and conflict-free.
- Content tombstones (§3.4) keep old events foldable.

### 8.2 Backend [open, recommendation]
Recommended: **Supabase** (Postgres + row-level security + auth + edge functions),
because RLS expresses "parent sees their child; coach sees team-scoped events" in the
database, SQL makes team reports cheap, it has a Kotlin Multiplatform client, and it
is self-hostable if needed. Alternative: Firebase (Auth + Firestore + FCM, GitLive
KMP SDK) - simpler auth and push, weaker for cross-learner aggregate queries. Either
way FCM is used for Android push. Record the choice as an ADR.

### 8.3 Server data model (outline)
`account(id, email, roles, created)`, `learner(id, nickname, avatar, age_band,
managed_by_parent_id?, own_account_id?)`, `consent(learner_id, parent_id, method,
at, version)`, `link(learner_id, adult_id, role, status)`, `team(id, owner_id, name,
variant, age_group, season)`, `team_member(team_id, learner_id, positions[],
groups[])`, `staff(team_id, account_id, role, groups[])`, `event(learner_id,
device_id, seq, payload jsonb, received_at)` (PK learner_id, device_id, seq),
`assignment(id, version, team_id?, created_by, target, pack_ids, items, options,
due)`, `playbook_play(id, team_id, version, diagram jsonb, jobs jsonb, items
jsonb)`, `report_subscription`, `moderation_report`.

### 8.4 Sync protocol
- Client keeps `lastSeqUploaded` per (learner, device). Upload is idempotent (PK
  conflict = already have it). Download is "events for this learner after cursor"
  plus assignment and playbook changes after a cursor.
- Sync runs on app open, after each lesson, and periodically via WorkManager
  (Android) when online. Never blocks the UI.
- Coach app keeps a local cache (SQLDelight) and computes dashboards client-side from
  events with the shared engine; server-side SQL views provide team aggregates for
  large rosters.

### 8.5 Security
RLS policies on every table; a learner device token is scoped to one learner;
coach queries can only read learners linked to their team via `team_member` and an
`approved` link; all writes validated in edge functions (event schema, note filters).
Penetration test and a privacy review before M3 public release.

---

## 9. Persistence and save format

- Local DB: **SQLDelight** (multiplatform) with tables `profile`, `event`,
  `snapshot`, `assignment_cache`, `playbook_cache`, `content_state`, `settings`.
- `schemaVersion` integer; every change ships a migration in
  `shared/data/migration/` with a test that migrates a fixture of every prior version.
- Backup export: a JSON file `{schemaVersion, profiles, events}` (DESIGN §9.9).
  Import validates and refuses newer schema versions with a clear message.
- Unlinked -> linked: local events upload; nothing is lost.

---

## 10. Accessibility and localisation
- Everything in DESIGN §12 applies to both apps. The coach dashboard heat map has a
  table alternative and never uses colour alone.
- Every diagram has a generated text description (`DiagramDescriber`, §11.2).
- **Localisation:** all UI strings in resources, all content strings in content
  JSON keyed by locale. English (US) at launch; Spanish (US) is the first planned
  locale **[open]**. Football terms keep English names with a localised explanation
  where that is how the sport is taught.
- Units: yards always (the sport's unit), explained once for metric-country users.

---

## 11. Architecture

### 11.1 Modules (target)

```
shared/
  core/        KMP commonMain: model, content loader + validator, engine
               (Grader, sessions, games, GradingRules), progress (Gating, Review,
               Placement, Mastery, Projection), DiagramDescriber, AssignmentPlanner,
               PlaybookQuizGenerator. No platform APIs.
  data/        KMP: SQLDelight DB, repositories, sync client (Ktor + Supabase),
               content updater, settings. expect/actual for platform bits.
  ui/          Compose Multiplatform: design system (DESIGN), FieldDiagram,
               step composables, games, shared screens.
androidApp-learner/   Android shell: Activity, navigation host, TTS, audio,
                      WorkManager, Play Billing (none in learner), Families config.
androidApp-coach/     Android shell for the coach app, Play Billing, FCM.
(later) iosApp-learner, iosApp-coach, webApp-coach
backend/              SQL migrations, RLS policies, edge functions, tests.
```
Dependency direction: `apps -> ui -> data -> core`. Nothing depends on an app;
`core` depends on nothing but kotlinx libraries.

### 11.2 Engine components (in `shared/core`)
- `Grader`, `LessonSession`, `SpeedRound`, `GameDay`, `LevelTest` (existing).
- `GradingRules`: tolerances for `draw_route`/`align` (named constants).
- `Mastery`, `Projection` (§4.6, §8.1).
- `AssignmentPlanner`: concept graph + mastery -> ordered plan (§7.4).
- `PlaybookQuizGenerator`: team play -> quiz steps (§7.5).
- `DiagramDescriber`: `Diagram` -> accessible text description (DESIGN §7.6),
  deterministic, locale-aware via templates in content.
- `DailySet`: seeded selection for daily practice and challenge.
- `Rng`: injected, seeded; the only source of randomness.

### 11.3 Multiplatform plan
1. **Done (M1):** `:engine` is now `:shared:core`, a Kotlin Multiplatform module
   with targets `jvm()` (tests, tools), `android {}` (the AGP KMP library plugin) and
   `iosArm64()`/`iosSimulatorArm64()`. The iOS targets are declared now, though they
   only link on macOS, because with only JVM-family targets Kotlin skips compiling
   `commonMain` on its own and `java.*` calls would slip into common code unnoticed
   (ADR-003). `jvmTest` depends on that common compile and on `checkDeterminism`
   (§11.5 rule 2), so `./gradlew :shared:core:jvmTest` enforces both rules.
   `ClasspathContentSource` lives in a `jvmShared` source set used by both jvm and
   android, so Android reads the content packaged in the library (no assets copy).
   `Math.round` became `roundToInt` (same rounding), and `Grader` now gives an empty
   step 0 credit instead of NaN. `core` stays free of Compose.
2. **Started (M1):** `:shared:ui` (Compose Multiplatform 1.9.3; jvm, android, iOS
   declared) holds the design tokens (`theme/`: palette, skins incl. Little Huddle, type
   scale, `GridironTheme`) and `FieldDiagram` with its pure, tested `FieldGeometry`.
   `ContrastTest` asserts the DESIGN §3 contrast promises in code.
   **UI in Compose Multiplatform from day one** (not Jetpack-only APIs), so the same
   screens run on iOS. Android-only concerns (TTS, WorkManager, billing, FCM,
   Families compliance) sit behind `expect`/`actual` interfaces in `data` or in the
   app shells.
3. **iOS (M5):** build the already-declared iOS targets on a macOS CI runner, SwiftUI shell hosting the
   Compose UI, AVSpeechSynthesizer for TTS, APNs for push, StoreKit for the coach app.
4. **Web coach (M5):** Compose for Web (Wasm) for the dashboard, or a small web client
   over the same Supabase views if Wasm is not ready [open].
5. CI builds every declared target so nothing platform-specific creeps into common
   code.

### 11.4 Tech stack
Kotlin 2.2.x; kotlinx.serialization, kotlinx-coroutines, kotlinx-datetime;
Compose Multiplatform + Material 3 (themed by DESIGN tokens); AndroidX
Lifecycle/ViewModel (KMP); Navigation Compose (KMP); Koin (DI); SQLDelight; Ktor
client; supabase-kt; Coil 3; multiplatform-settings; Android TextToSpeech; Media3
for audio; WorkManager; Play Billing (coach app); FCM. JUnit 5 + kotlin.test;
Paparazzi or Roborazzi for screenshots. minSdk 26, target latest.

### 11.5 Code rules (AGENTS.md is the short version)
1. `shared/core` has no platform imports, no `java.*` outside `jvmMain`.
2. No `Math.random`, `java.util.Random`, `Random.Default`, wall-clock or UUID calls in
   `core`; randomness from `Rng`, time and ids passed in. The `checkDeterminism` Gradle
   task fails the build on `Random`, `kotlin.random`, `Clock.System`, `TimeSource` or
   `Uuid.random` in `commonMain`; the common-only compile rejects `java.*`.
3. Lesson text, voice lines, schemes, packs and glossary are content JSON, never
   Kotlin.
4. Models are immutable `data class`es with `val` only.
5. Every graded step has an explanation; every concept id exists in the glossary and
   concept graph.
6. Thresholds and tolerances are named constants (`GradingRules`, `MasteryRules`,
   gate fields in content), never inline numbers.

### 11.6 Decisions to record in `docs/DECISIONS.md`
- ADR-001 Two apps, not one app with modes (§1.2).
- ADR-002 Event-sourced progress (§8.1).
- ADR-003 KMP + Compose Multiplatform from the start (§11.3).
- ADR-004 Backend choice (§8.2).
- ADR-005 Level 0 has no test (§4.4).
- ADR-006 Recorded voice for Level 0 (§6.9).

---

## 12. Analytics
- Unlinked profiles: no analytics by default. An opt-in (grown-up gate) shares
  anonymous, aggregate item statistics: step id, result, time bucket, level, age band.
  No device id, no profile id, batched daily.
- Linked learners: the server already has their events (for their adults); content
  QA uses only aggregated, de-identified statistics from them.
- Content QA dashboard (internal): per-item p-value, discrimination, average time,
  distractor choice rates; flags items outside 0.3-0.97 p-value for rewrite.
- No third-party analytics or advertising SDKs in the learner app.

---

## 13. Quality

### 13.1 Unit tests
`./gradlew :shared:core:jvmTest` must pass before any change. Every engine change
has a test. Determinism tests: a recorded event log folded twice gives identical
projections; seeded game sets are stable across releases (golden files).

### 13.2 Content tests
`ContentTest` runs `ContentValidator` over the bundle (§3.3). A packs test plans every
pack for an empty-progress learner and asserts each plan is finite and only uses
existing steps.

### 13.3 Football accuracy review
Every step needs a sign-off in `review.json` from a reviewer with coaching
experience before release (kid-facing first). Rule-dependent steps re-reviewed each
offseason against the stamped `ruleSeason`. Release builds exclude unreviewed steps;
debug builds show them with a "[review]" badge.

### 13.4 UI tests
- `FieldRenderTest` (jvm) already renders real content diagrams headlessly to
  `shared/ui/build/field-renders/`. Render tests run with a one-folder fontconfig
  (DejaVu until the brand fonts are bundled): Skia's default font scan stalled for
  10+ minutes on a machine with ~2,800 fonts, and a fixed font set makes PNGs
  identical across machines.
- Screenshot tests of the Design gallery (DESIGN §13) in both skins, both themes, at
  100% and 200% font scale, plus every step type and every `PathKind`.
- Compose UI tests for the lesson player, each game, linking and assignment flows.
- Accessibility checks (contrast token script, TalkBack labels) in CI.

### 13.5 Backend tests
RLS policy tests (a coach cannot read an unlinked learner; a parent cannot read
another family; revoked links stop access immediately), sync idempotency, migration
tests.

### 13.6 Performance budgets
Cold start < 1.5 s on a mid-range 2022 Android phone; lesson step transition < 100 ms;
`FieldDiagram` at 60 fps during a reveal; APK < 40 MB including Level 0 audio for one
locale; content bundle load < 300 ms; dashboard of 60 learners x 40 concepts computes
in < 500 ms on device.

---

## 14. Release plan

| Milestone | Scope | Exit criteria |
|---|---|---|
| **M0** (done) | Engine, content schema, Kid L1 and Adult L1 | Tests green |
| **M1** Learner MVP | KMP move; Compose UI for all v1 step types and 4 games; Home, Level, player, review, glossary, profiles, grown-up zone; Level 0 (4 units with recorded audio); Kid/Adult L2 content | Internal testing track; 10 families playtest; DESIGN screenshots pass |
| **M2** Depth | L3-L5 content; placement; mastery + Playbook map; new step types (`draw_route`, `align`, `film`, `spot_foul`, `call_it`); content OTA updates | Closed beta (100 users); item stats within bands |
| **M3** Coach MVP | Backend, accounts, VPC, linking, parent + coach dashboards, scheme packs, assignments, assignment inbox; privacy and legal review | Public launch of both apps (Android); 5 real teams onboarded |
| **M4** Team | Team playbook builder + generated quizzes; groups/positions; reports and digests; L6-L7 (full disguise unit); team board | 20 teams through one season |
| **M5** Platforms | L8 Architect; iOS learner + coach; web coach dashboard; Spanish | Store releases on iOS; parity checklist |

---

## 15. Open questions
1. **Age floor:** Level 0 targets 4-7. Is 4 the right floor, or start at 5? (Affects
   audio scope and Families age ratings.)
2. **Monetization** (§5.3): free core + Family/Team plans bought in the Coach app,
   or fully free / one-time purchase?
3. **Backend:** Supabase (recommended) or Firebase (§8.2)?
4. **VPC method and vendor** for under-13 linking; legal counsel needed (§5.4).
5. **Default variant for kid content:** flag (5v5) or youth tackle? Current plan:
   flag first, tackle-specific steps tagged `youth`.
6. **Video:** should coaches eventually upload their own game film clips? (Large
   privacy and moderation cost; out of v1.)
7. **Voice talent** for Level 0 recordings and the first second locale.
8. **Teacher role** (classroom use) and whether schools are a market.
9. **Name check:** confirm "Gridiron Academy" is clear to use on both stores.
10. **Web coach dashboard:** Compose Wasm vs a thin web client (§11.3).
