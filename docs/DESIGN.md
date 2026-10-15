# Gridiron Academy — design system: Playbook

The code cites this file by section. Change the section in the same commit as the
code it describes. Colour ratios below were **measured** (WCAG relative luminance,
scripted), not eyeballed; if you change a hex, re-measure.

## 1. Principles

1. **The field is the canvas.** Football is a spatial game, so teaching is spatial.
   Wherever an idea can be shown on a field, it is shown on a field before it is
   described in words. Text explains the picture, not the other way round.
2. **Colour encodes role, never decoration.** Offense is blue, defense is
   orange, a pass is a white line, a blitz is a hot line. Every colour lives in
   `ui/theme/Color.kt`; no composable writes a hex value. Colour is **never the
   only carrier**: side is also shape, path kind is also line style, a right
   answer is also an icon and a word.
3. **A wrong answer is a teaching moment, not a penalty.** No lives, no hearts,
   no red, no buzzer, no countdown on the first attempt at a lesson. The screen
   says "Not quite", shows why, and moves on.
4. **Two voices, one system.** The Kid skin and the Adult skin share every token,
   component and diagram; they differ in scale, shape, copy and illustration
   (section 2). A kid who "graduates" to Level 4 gets the shared content in the
   shared skin without the app feeling like a different product. Level 0 is a mode
   of the Kid skin, not a third skin (section 2.1), and the Coach app reuses the
   Adult skin (section 14).
5. **Big, calm, finger-friendly.** One primary action per screen, in gold.
   Touch targets are generous (64dp Little Huddle, 56dp Kid, 48dp Adult). Nothing important is
   reachable only by a gesture; nothing animates unless it teaches.
6. **Original everything.** No real club, logo, player, broadcast package or
   copied diagram. Teams are generated letterform marks (`TeamMark`); the
   mascot and illustrations are original (section 6).
7. **Reachable in two taps from Home**: any lesson, the daily practice, the
   glossary, settings. The grown-up zone is deliberately three (section 9.9).

## 2. Two skins, one system

The skin is chosen by the **profile's track** (`Track.KID` / `Track.ADULT`) and
applies to levels 1-3. Shared levels (4-8) use the profile's track skin too, so a
learner never changes look mid-journey; the *content* of shared levels is the same.

| | **Pee-wee** (Kid) | **Film Room** (Adult) |
|---|---|---|
| Mood | Sticker book, sunny, bouncy | Coach's tape session, focused, quiet |
| Corner radius | 20dp cards, 16dp buttons, full-round chips | 10dp cards, 8dp buttons, 4dp chips |
| Body text | 20sp, line height 28sp | 16sp, line height 24sp |
| Display type | Fredoka (rounded) W600 | Barlow Semi Condensed W700 |
| Touch target | 56dp | 48dp |
| Icons | Filled, 28dp, 2dp rounded stroke accent | Outline, 24dp, 1.5dp stroke |
| Mascot | **Clip** is present: on cards, in feedback, in empty states | **Clip** appears only on the welcome card and milestones, as a small mark |
| Copy | Short, warm, exclamation allowed, second person | Plain, direct, no exclamation marks except milestones |
| Motion | Springy; confetti on a level pass; 1.0x durations | Linear-out; a gold underline sweep on a pass; 0.7x durations |
| Sound | Cheerful chimes, crowd "yay" on a level pass | A soft click and a single tone; off by default |
| Read-aloud | On by default | Off by default |
| Diagram players | Helmets with jersey numbers | Discs with position labels |

Everything not in this table is identical. A component is written once and reads
the skin from `LocalSkin.current`.

### 2.1 Little Huddle mode (Level 0, ages 4-7)
Level 0 is for children who cannot read yet (spec 3.1, 6.9). It is the Kid skin with
these overrides, carried as `Skin.littleHuddle = true`:

| | Little Huddle |
|---|---|
| Touch target | 64dp; answer pictures at least 120dp square |
| Text | No text-only element anywhere. Titles may show, but every one is also spoken and has a picture |
| Voice | Recorded voice plays each prompt automatically; a 64dp speaker button replays it. TTS is a fallback only |
| Answers | Pictures (`PictureCard`, section 8), at most three per screen |
| Feedback | No "Not quite". A miss plays "Let's try that one together", highlights the right picture with a gentle pulse, and the child taps it to go on |
| Rewards | Stickers, not stars or XP numbers. No score is ever shown |
| Timers | None |
| Navigation | One action per screen, no back-stack deeper than the lesson; the close button needs a 2-second hold so a stray tap doesn't quit |
| Motion | Kid motion, slower (1.2x durations); reduced motion rules still apply |

The mode applies only inside Level 0. A 4-7 profile that moves on to Kid L1 gets the
normal Kid skin.

## 3. Palette

Two themes, both available to both skins: **Daylight** (light) and **Floodlight**
(dark). The default follows the phone; the grown-up zone and the profile editor can
override it. The **diagram field is dark turf in both themes** (a light field
washed out every side colour; see the measurement in section 7.1).

| Role | Daylight | Floodlight | Use |
|---|---|---|---|
| surface | `#F7F4EB` | `#0F1B16` | the ground of every screen |
| surfaceRaised | `#FFFFFF` | `#172A22` | cards, sheets, answer buttons |
| line | `#D9D3C3` | `#2C4439` | hairlines, disabled |
| ink | `#10201A` | `#F3F0E7` | primary text |
| inkDim | `#4A5850` | `#AEBDB3` | secondary text |
| gold | `#F2B01E` | `#F2B01E` | the one action a screen is for; stars; streak |
| onGold | `#1B1400` | `#1B1400` | text on gold |
| offense | `#1D4ED8` | `#7DA8FF` | offense: text and marks on surface |
| defense | `#C2410C` | `#FF9468` | defense: text and marks on surface |
| correct | `#176B3A` | `#5BDB92` | a right answer: text, icon, edge |
| retry | `#8A5A00` | `#F5C451` | "not quite": text, icon, edge. Amber, not red |
| link | `#14532D` | `#8FE3B0` | links, glossary terms |

Measured contrast (text on `surface` / `surfaceRaised`):

| Pair | Daylight | Floodlight |
|---|---|---|
| ink | 15.35 / 16.89 | 15.50 / 13.25 |
| inkDim | 6.81 / 7.49 | 9.03 / 7.72 |
| offense | 6.09 / 6.70 | 7.50 / 6.41 |
| defense | 4.71 / 5.18 | 8.15 / 6.97 |
| correct | 5.97 / 6.56 | 10.09 / 8.62 |
| retry | 5.39 / 5.93 | 10.85 / 9.27 |
| link | 8.28 / 9.11 | 11.59 / 9.91 |
| onGold on gold | 9.60 | 9.60 |

Every text pair clears **4.5:1** in both themes (lowest: Daylight defense on
surface, 4.71). Gold is a **fill**, never text on surface (1.74:1 on Daylight): it
always carries `onGold` text at 9.6:1.

### 3.1 Diagram palette (identical in both themes)

| Token | Hex | On turf `#1F5A3A` / `#1A4E32` | Use |
|---|---|---|---|
| turfA / turfB | `#1F5A3A` / `#1A4E32` | | field stripes, 5 yards each |
| chalk | `#F3F0E7` | 7.14 / 8.45 | yard lines, hash marks, LOS, pass routes, outlines |
| offenseFill | `#1D4ED8` | white label 6.70 | offense disc fill (Kid helmet shell) |
| defenseFill | `#B73A07` | white label 5.80 | defense square fill |
| routeRun | `#F2B01E` | 4.26 / 5.05 | run paths |
| routeBlitz | `#FF9468` | 3.75 / 4.44 | blitz and pressure paths |
| routeRotate | `#8FE3FF` | 5.65 / 6.70 | safety and corner rotations |
| routeDrop | `#C9B8FF` | 4.56 / 5.41 | zone drops |
| zoneFill | `#FFFFFF` at 18% | | coverage zones, gaps, the box |
| zoneEdge | `#FFFFFF` | 8.13 / 9.63 | zone outline, 1.5dp dashed |

Path colours are chosen for **3:1 as graphics** on both turf stripes; the lowest is
the blitz path on turfA at 3.75:1. Every marked player also wears a chalk ring, 7.14:1
against turf, so a disc's *edge* is always legible even where its fill is not
(blue on green is 1.3:1 and is why the ring exists).

## 4. Type

- **Body and all data:** Atkinson Hyperlegible, chosen for letter-shape
  distinctness (it was designed for low vision and is kind to dyslexic readers).
  Regular and Bold.
- **Display, Kid:** Fredoka W600. **Display, Adult:** Barlow Semi Condensed W700.
- The fonts are bundled (OFL, `docs/licenses/fonts/`) as Compose resources in
  `shared/ui`. Fredoka is the variable font with its weight axis pinned to 600.
  Player labels on the field use Atkinson Hyperlegible Bold.
- Tabular figures wherever a number is compared (`fontFeatureSettings = "tnum"`),
  so a scoreboard or a down-and-distance line never jitters.
- All sizes are `sp` and scale with the system. **Nothing breaks at 200%** (section 12).

| Style | Kid | Adult | Use |
|---|---|---|---|
| displayLarge | 40/48 | 36/40 | level complete, "Touchdown!" |
| headline | 28/34 | 24/30 | screen title |
| title | 22/28 | 20/26 | card title, lesson title |
| body | 20/28 | 16/24 | lesson text, prompts, explanations |
| answer | 20/26 bold | 17/24 bold | answer button text |
| label | 16/20 | 14/18 | chips, captions, tabs |
| scoreboard | 28/28 tnum | 24/24 tnum | HUD numbers |

Line length in a card is capped at about 38 characters in Kid and 60 in Adult.

## 5. Shape, space, elevation

- **4dp grid.** Screen gutter 16dp (20dp Kid). Card padding 16dp (20dp Kid). Gap
  between answer buttons 12dp.
- **Elevation is a tint, not a shadow:** `surfaceRaised` plus a 1dp `line` border.
  The one shadow is the pressed-in state on an answer button (inner, 2dp).
- **Radii** per skin as in section 2. Sheets round the top corners at 24dp (Kid)
  or 12dp (Adult).
- **Buttons.** Primary: gold fill, `onGold` text, 56/48dp tall, full-width on
  phones. Secondary: transparent, 2dp `ink` outline. Tertiary: `link` text, no
  container, 48dp hit area. A screen has **at most one primary**.

## 6. Mascot, icons, art

### 6.1 Clip
**Clip** is a friendly clipboard with a whistle on a lanyard: two eyes and a
mouth drawn on the board, a pencil for an arm. Original, no resemblance to any real
mascot. Poses (each a vector layer set): `hello`, `think`, `cheer`, `oops` (a
sheepish tilt, not a sad face), `whistle` (start of a game), `flag` (rule
explained). Clip speaks in the feedback sheet: Kid always, Adult never except at
milestones.

### 6.2 Icons
Thirty-eight glyphs on a 24dp grid: home, drill, checkpoint, big game, star, streak,
speaker, speaker-off, back, close, settings, glossary, timer, lock, check, retry,
hint, flag, whistle, play, replay, share, profile, grown-up, calendar, cleats, ball,
helmet, plus (v2) assignment, team, link, sticker, playbook, pencil (draw), eye
(film), mastery, report, kudos. Kid filled, Adult outline, one source (`Icons.kt`)
with a stroke/fill switch. Mastery states each have their own glyph (section 8.1).

### 6.3 Art production
Launcher icon, store art and illustrations are generated with Nano Banana (via the
`nanobanana` skill) and kept as source in `docs/play-store/assets/source/`.
`draw-assets.py` builds every density from them. The icon is a **gold football with a
chalk play-diagram (an X and an arrow) in place of laces**, on a turf-green
adaptive background. The **Coach app icon** is the same gold football on a
`surface` Floodlight (`#0F1B16`) background with a small chalk clipboard clip at the
top, so the two apps read as a family but are told apart at a glance. The Kid title
art is Clip in a stadium at golden hour; the Adult title art is a night-game press-box
view of an empty field under floodlights. Level 0 needs a picture set (ball, two
teams, end zone, referee, flag belt, helmet, mouthguard, high-five) in the same flat
style as Clip.
Store icon is a 32-bit RGBA PNG at 512 px; feature graphic 1024x500, no alpha.

## 7. The field renderer

One composable, `FieldDiagram(diagram, mode)`, draws every picture in the app: a
lesson card, a tap-the-player quiz, a read-the-defense pre-snap and its reveal. It is
the most important component in the product, so it is specified closely.

### 7.1 Coordinates and fit
Input is the engine's `Diagram`: x across in yards (0 = centre, left negative), y
downfield from the line of scrimmage (offense moves up the screen). The renderer
maps x:[-26.67, 26.67] and y:[-behind, +depth] onto the available rectangle.

**Auto-fit** (`FieldGeometry`). Compute the tight bounds of all players, route points
and zones, always including the line of scrimmage, add a 3-yard margin, widen to at
least 20 x 10 yards (so one player is not a giant close-up), and clamp to the field
width, shifting the window rather than squashing it. A diagram with nothing on it
shows `-behind..depth` across the whole field. A zone spans `x..x+w` across and
`y..y+h` downfield. The scale is `min(w/xSpan, h/ySpan)`,
and the picture is centred; turf fills the whole canvas and the canvas clips. A formation-only diagram (no routes) crops tighter than
a deep route concept. Aspect: the lesson card allows 4:3 to 3:4; the game screens
let the diagram fill all remaining height.

**Why the field is dark in both themes.** Measured on a light field (`#2F7D4F`), the
offense blue was 1.33:1 and the defense orange 1.03:1. A dark field plus a chalk
ring gives 7.14:1 outlines and 3.75:1 or better paths, in both themes.

### 7.2 Field dressing
- Stripes alternate `turfA` / `turfB` every 5 yards.
- Yard lines every 5 yards in chalk at 35% opacity, every 10 yards at 60%; the
  line of scrimmage is a **solid 2dp chalk line** with a small "LOS" tag at the left
  end in Adult skin only.
- Hash marks every yard at the hash lines (+-3.1 yards), 4dp long, chalk at 40%.
- The sideline is a 3dp chalk line when the crop reaches it. An end zone, when in
  frame, is `turfB` with a 40% chalk diagonal hatch.
- Optional **yard numerals** (Kid skin) printed in chalk at 50% on each 10.
- Caption: one line, `label` style, under the field, from `Diagram.caption`.

### 7.3 Players

| Side | Shape (Adult) | Shape (Kid) | Fill | Label |
|---|---|---|---|---|
| Offense | circle, 22dp | helmet (circle plus facemask), 30dp | `offenseFill` | white, bold, 11sp min (13sp Kid) |
| Defense | rounded square, 22dp | helmet, flipped, 30dp | `defenseFill` | white, bold |

- Every mark has a 2dp chalk ring. Labels are position letters or jersey numbers
  as authored.
- **Marks shrink to fit.** A full-width formation on a 360dp phone puts linemen about
  10dp apart. All marks share one size: the skin's size, shrunk so the closest two
  players are 2dp apart, never below **18dp**; labels scale with them, never below
  **9sp**. Players aligned within a yard of each other (DL on OL, QB under centre)
  can still touch at phone width; tap questions then offer list mode (below).
- **Touch target is 48dp (56dp Kid, 64dp Little Huddle) around the centre**
  regardless of the drawn size.
  Overlapping targets resolve to the nearest centre; if two centres are under 40dp
  apart the screen-reader list mode (section 7.6) is offered as an alternative.
- A selected mark gains a 3dp gold outer ring and a scale of 1.15 (no scale under
  reduced motion). A **correct** target after grading gets a `correct` ring and a check
  badge; a wrongly chosen one gets a `retry` ring and a dash badge; a missed target
  pulses once. The badge is drawn, so colour is never the only signal.

### 7.4 Paths

| Kind | Line style | Colour | End |
|---|---|---|---|
| `pass` | solid 3dp | chalk | arrowhead |
| `run` | solid 4dp | `routeRun` | arrowhead |
| `motion` | zig-zag 3dp (amplitude 1.2dp, period 8dp) | chalk | arrowhead |
| `block` | solid 3dp | chalk | a short perpendicular bar (the "T") |
| `blitz` | solid 4dp | `routeBlitz` | arrowhead, filled |
| `rotate` | dashed 3dp (8 on, 5 off) | `routeRotate` | arrowhead |
| `drop` | dashed 3dp (3 on, 4 off) | `routeDrop` | arrowhead |

A route's points are waypoints after the player's start; corners are rounded with a
radius of one yard so a "curl" reads as a hook and an "out" as an out. A route's
`label` is drawn at its end, `label` style, in a 70%-opaque `surface` pill.

**Layer order**, back to front: turf and lines, zones, paths, zone and route labels,
the LOS tag, players. A label never hides a player. The Adult "LOS" tag sits just
under the line at the left end, moves to the right end if a player is there, and is
left out if both ends are taken. A legend
row (kind icon plus name) appears once in a lesson when the lesson introduces a
path kind, then is dropped.

### 7.5 Zones
Rectangles at 18% white with a dashed 1.5dp white edge and the zone's label centred
in `label` style on a small pill. Coverage zones overlap legibly because fills are
translucent and edges are drawn. When more than four zones are shown, only the
zone being discussed is filled; the rest show edges only.

### 7.6 Accessibility of a diagram
Every `Diagram` produces a **text description** (`DiagramDescriber`, in the engine,
section 11.2 of the spec): "Offense in shotgun: quarterback 5 yards behind the
center; running back beside him; three receivers wide left, one wide right..." plus
the routes and zones. The canvas exposes that text as its content description. Tap
questions also render a **list mode** (a vertical list of players by name and
position) selectable in Settings and offered automatically when TalkBack is on; it
grades identically.

### 7.7 The reveal
`read_defense` steps show the pre-snap picture, take an answer, then animate the
reveal: players travel along their `rotate`, `drop` and `blitz` paths over 900ms
(630ms Adult), ease-in-out, staggered by 60ms in the order of the `routes` array so
the *safeties* move first and the eye follows the rotation. The pre-snap alignment
ghosts at 25% opacity at each player's start. Under **reduced motion** the picture
cross-fades to the end state in 150ms and the ghosts remain. A **Replay** button
(icon plus label) replays the reveal; **Show both** overlays the two states.

### 7.8 Film mode
`film` and `spot_foul` steps (spec 4.2) play a whole play: every path animates from
the snap over its own timing, at 1x (default), 0.5x or frame-step. Controls sit under
the field: play/pause, replay, speed chip, and a scrubber with the snap marked.
`spot_foul` pauses on tap and asks "Who?"; the tapped player then gets the selection
ring. Reduced motion shows numbered end-state snapshots (snap, 1s, 2s, end) instead
of continuous movement.

### 7.9 Drawing and placing
- **`draw_route`:** the learner drags from the highlighted player; the stroke is
  drawn as a `pass` path in gold while drawing, then simplified to waypoints and
  snapped to whole yards. **Undo** and **Clear** buttons sit beside **Check**. The
  tap alternative (no drag): tap the player, then tap up to four points, then Done.
  After grading, the target route appears in chalk beside the learner's in
  `correct` or `retry` with its badge.
- **`align`:** the player to place carries a 4-way handle; drag, or tap the player then
  tap the spot. A faint 1-yard grid and the technique/gap labels (Adult) appear while
  placing. Graded within the tolerance in `GradingRules`; the right spot is ghosted
  after grading.
- **Play editor** (Coach app, section 14.5) uses the same two interactions.

## 8. Components

| Component | Spec |
|---|---|
| **LevelNode** | A 72dp circle on the home map. States: locked (line fill, lock icon), open (gold ring, level number), in progress (progress arc in gold), passed (solid gold, check, star count beneath), "test out" (an arrow badge, Adult only) |
| **DrillCard** | Raised card: title, blurb, 3-star row, 4dp left edge in `correct` when passed, `line` when locked |
| **DriveBar** | The lesson progress indicator: a horizontal field strip, the ball advancing from the 0 to the opposite end zone as steps complete, yard ticks every 10. Replaces a plain bar. Determinate, `semantics: progress` |
| **AnswerButton** | Full-width, surfaceRaised, 2dp `line` border, `answer` text, left letter chip A/B/C/D. States: rest, focused, selected (gold border), correct (`correct` border, check, "Correct" in the TalkBack label), retry (`retry` border, dash icon), disabled |
| **FeedbackSheet** | Bottom sheet after every graded step: icon, one-line verdict ("Got it!" / "Not quite"), the explanation, the Continue button (primary). Kid: Clip pose beside the verdict. Never covers the diagram's key area: the sheet's height is capped at 45% and the diagram scrolls up |
| **StarRow** | Three stars, 1/2/3, gold fill or `line` outline, with a text "2 of 3 stars" label |
| **StreakChip** | Flame icon plus day count; tapping opens the week strip (M-S dots). The weekly rest day (spec 4.7) shows as a small bench icon, not a gap. Missing a day never shows red; a broken streak is shown as "Start a new streak" |
| **ScoreBug** | The game HUD: score, quarter, clock, down and distance, ball position. Used by Game Day. Looks like a small broadcast bug but original |
| **SituationStrip** | Down & distance as a large `scoreboard` line ("3rd & 4"), the field position on a DriveBar, the clock, the score difference, timeouts as pips |
| **TimerRing** | Speed round: a gold ring draining over the limit, plus the seconds as text; under 10s the ring thickens (not turns red) and a tick sounds |
| **MatchBoard** | Two columns of chips; tap-tap to pair (drag is optional); a paired chip shows its partner's letter; a wrong pair shakes once (or flashes, reduced motion) and releases |
| **OrderList** | Items as chips with up/down buttons and drag handles; drag is optional |
| **GlossaryChip** | A term in lesson text underlined in `link` with a dotted rule; tap opens the term sheet (Kid wording by default, "Full definition" expands) |
| **LevelCompleteBanner** | Full-width, "Touchdown!" in displayLarge, stars, the new level node animating to passed |
| **TeamMark** | A generated two-letter monogram in a pennant or shield shape from a small set; used for fictional teams in situation and game screens, and for real teams in the Coach app (generated from the team name; no uploads) |
| **PictureCard** | Little Huddle and kid answers: a square card with a picture, no text, spoken label on focus and on tap-and-hold. States as AnswerButton |
| **StickerBook** | Grid of earned stickers, one per Level 0 / kid lesson; unearned slots are outlined silhouettes, never greyed-out "missing" |
| **AssignmentCard** | Assignment title, "From Coach Ray" (or parent name), due date (relative: "Due Friday"), progress ring, the note in quotes. Overdue shows a `retry` dash icon and "Past due", never red |
| **VariantChip** | Small chip on a step saying which rules it means: NFL, College, High school, Youth, Flag. Shown only when the step's variant is not `any` |
| **MasteryNode** | A concept on the Playbook map: circle with the state glyph and the concept name (section 8.1) |
| **ReviewBadge** | Debug builds only: "[review]" on a step not yet signed off (spec 13.3) |

### 8.1 Mastery states
Mastery (spec 4.6) is shown everywhere with the same glyph, word and colour, so colour
is never the only signal:

| State | Glyph | Colour | Word |
|---|---|---|---|
| New | empty circle | `line` | "New" |
| Learning | quarter-filled circle | `inkDim` | "Learning" |
| Practiced | half-filled circle | `offense` | "Practiced" |
| Mastered | filled circle with check | `correct` | "Mastered" |
| Rusty | filled circle with a small dash | `retry` | "Needs a refresher" |

## 9. Screens

Wireframes are schematic. `[ ]` is a button, `( )` a chip, `====` the DriveBar.

### 9.1 Home: the drive map
The course is a **drive down the field**. A vertical scrolling field strip; the eight
levels are nodes along it, 12.5 yards apart, from your own goal line at the bottom to
the opponent's end zone at the top. The kid and adult tracks diverge for levels 1-3
(two parallel lanes, the other track's lane greyed out and not shown in Kid) and
merge at the 50, where Level 4 sits.

```
 +--------------------------------+
 |  [profile]   Gridiron   [flame 5]|
 |                                |
 |        ( 8  Architect )  lock  |
 |        ( 7  Coordinator ) lock |
 |        ( 6  Pro )        lock  |
 |        ( 5  College )    lock  |
 |   - - - 50 - - - - - - - - - - |
 |        ( 4  Varsity )   gold   |
 |   ( 3 )          ( 3 )         |   <- tracks diverge below the 50
 |   ( 2 )          ( 2 )         |
 |   ( 1 )  done    ( 1 )         |
 |  ---- goal line ------------   |
 |  [ Today's practice  6 cards ] |  <- the one primary
 +--------------------------------+
```
**Level 0** (kid profiles aged 4-7 only) is a **practice field** pad drawn behind
the goal line, below Level 1, in Little Huddle style. It is hidden for adult and
8+ profiles.

When a linked adult has assigned something, an **Assignments strip** (one
AssignmentCard, plus "2 more" if there are others) sits above the primary action.

Tapping a node opens the **Level screen** (9.2). The primary action is, in order:
**Start assignment** if one is overdue; **Today's practice** (spaced review, section
6.4 of the spec) when cards are due; else **Continue** to the next open drill.

### 9.2 Level screen
Title, tagline, the level's DriveBar, then units as sections, each with its DrillCards in
order. After the last drill: a **Checkpoint** card (the level test), then the **Big
Game** card (the level game). Locked cards show "Pass *Drill name* to unlock". For
Adult levels, a **"Test out"** secondary button on the Checkpoint card. A drill
unlocked early by an assignment shows an "Assigned by *name*" badge instead of a
lock.

**Level 0** has no Checkpoint: its lessons are big picture tiles (not DrillCards),
and the last tile is **Picture Play** (spec 4.3).

### 9.3 Drill player (any step)
```
 +--------------------------------+
 | [x]  ====o=========    [spk]   |  DriveBar, close, read-aloud
 |                                |
 |  Prompt text, wrapped at       |
 |  38 / 60 characters            |
 |  +--------------------------+  |
 |  |      FieldDiagram        |  |  only when the step has one
 |  +--------------------------+  |
 |  ( A )  Answer one             |
 |  ( B )  Answer two             |
 |  ( C )  Answer three           |
 |                                |
 |  [ Check ]                     |  primary, appears after a selection
 +--------------------------------+
```
`learn` steps show the card and **Next**. `true_false` shows two large buttons,
**True** and **False**, each with its icon. `match`, `order` and the tap steps use
their components and a **Check** button. After Check, the FeedbackSheet rises. A step
missed on the first try is requeued once at the end; the DriveBar shows
the retried steps as hollow ticks. A **hint** (Kid skin) is a second tap on the
speaker: it re-reads the prompt slowly.

The v2 step types (spec 4.2) use the same frame:

| Step | Body |
|---|---|
| `picture_choice` | Two or three PictureCards in a row (2x2 grid for four); prompt spoken |
| `drag_to` | The field or scene with a draggable item and highlighted drop spots; tap-then-tap alternative |
| `listen_tap` | A scene; each tappable thing speaks its name and wiggles once. Ungraded, so **Next** shows after three taps |
| `draw_route`, `align` | FieldDiagram fills the space; controls per section 7.9 |
| `film`, `spot_foul` | FieldDiagram in film mode (section 7.8), then the choices |
| `call_it` | SituationStrip (if the step has a situation), the defense diagram, then up to four play names as AnswerButtons, each with a mini route thumbnail |
| `my_job` | The team play diagram with the learner's position ringed in gold, the question ("What's your job on *Buck Sweep*?"), then the answer control the item uses |

A VariantChip sits under the prompt when the step's rules variant matters.

### 9.4 Read the defense
Pre-snap picture fills the screen under a one-line prompt and the choices (the choices
are a horizontal chip row of up to four, or a two-by-two grid in Kid). After Check the
**reveal** plays (7.7), then the feedback sheet states what the defense did and which
clue pointed to it. A disguised item adds a single badge, "Disguise", so a learner
knows the shell may lie, and the explanation always ends with the clue.

### 9.5 Game Day
A full-bleed ScoreBug at the top, the SituationStrip beneath, the field diagram when the
step has one, the options as large buttons. After a decision, the sheet shows the
verdict (**Best call / Reasonable / Costly**, not "right/wrong"), the why for the option
chosen, and the explanation. The game ends with a **box score**: coach points, best
calls, and the single decision that cost the most, replayed.

### 9.6 Speed round
Timer ring, streak chip, a score counter, and one question at a time with two to four
answer buttons. Answers advance immediately with no sheet; the explanation for a miss
is collected and shown in a review list at the end. Ends with accuracy, best streak and
points.

### 9.6.1 Other games
- **Picture Play** (Level 0): six Little Huddle rounds; each right answer puts a
  sticker on a small field; the end screen is the field full of stickers and Clip
  cheering. No score.
- **Route runner:** the defense is shown; the learner draws the route (7.9). The
  HUD counts "routes won".
- **Ref school:** film-mode plays (7.8); the HUD is a small flag icon per play.
- **Playbook quiz:** like a lesson, but the DriveBar is labelled with the play name and
  the end screen lists plays as "Know it" / "Review".
- **Daily challenge:** a lesson-style run with the date in the title and a single
  "today's score". Team-linked learners see where they sit on the team board (if
  the coach turned it on); others see only their own score.

### 9.7 Level complete
"Touchdown!" (Kid) or "Level 3 complete" (Adult), the stars earned for the Checkpoint
and Big Game, the level node animating to passed, and one primary: **Next level** (or
**Back to the map** if the next level is gated by the other track's convergence). For
the final level: a credits-style **Hall of Fame** page of every concept mastered.
Level 0 ends with "You did it!" spoken by Clip, the full StickerBook page, and a
**Next** that goes to Kid L1 (the grown-up can choose to wait).

### 9.8 Placement and onboarding
First launch: a welcome card with Clip, then **Who's playing?** (nickname only,
never a real name; avatar of eight helmets), then the **age screen** (spec 5.2): "How
old is the player?" with six equal buttons, 4-5, 6-7, 8-9, 10-12, 13-17, 18+, no
default, no hint about what each unlocks. The band sets the track and start:
4-7 -> Level 0; 8-12 -> Kid L1; 13+ -> Adult L1. The skin follows. Profiles of 10+ see
an optional **"I know some football"** placement: one 6-question quiz per level,
stopping at the first level missed (80% to pass), starting them there with earlier
levels marked passed.

### 9.9 Grown-up zone
Behind a **grown-up gate** (hold-to-open plus a multiplication question; never
arithmetic a young child could guess, never a time-based hold alone). Contents:
**Profiles** (add, rename, delete), **Time** (a daily practice limit that ends a
session politely), **Reading aloud**, **Sound**, **Theme**, **Text size**, **Progress report**
(concepts mastered per level, drills passed, practice minutes),
**Backup** (export and import a progress file), **Link to a coach or parent**
(scan a QR or type the 8-character code from the Coach app; spec 7.2), **Linked
adults** (who can see this profile; 13+ profiles can leave a team here), **Content
updates** (current content version and rule season), **Privacy** (a plain statement:
"Nothing about this player leaves this device unless a parent links it in the Coach
app." Once linked: what is shared and with whom), **About**.

There is **no Purchases screen** in the learner app (spec 5.3). Where a paid level
would be, the learner sees "Ask a grown-up" with no price; the grown-up zone explains
that plans are managed in the Coach app.

### 9.10 Glossary
Alphabetical and by level, searchable. Each term: Kid-wording headline, "Full
definition" expands, "Where you learned it" links to the first drill that teaches it,
and a **Quiz me** that starts a mini-drill from that concept's graded steps.

### 9.11 Empty and error states
No screen is blank. First-run Home has Clip pointing at the profile's first level
(Level 0 or Level 1). A content file that
fails to load (should never ship; section 3.3 of the spec) shows a kind error with
**Try again** and writes a local diagnostic; the app never crashes on bad content.

### 9.12 Assignments inbox
Reached from the Assignments strip on Home or the profile menu. A list of
AssignmentCards: active first (soonest due), then done. Opening one shows its
**plan** (spec 7.4) as a short vertical DriveBar of stops: "Warm-ups" (prerequisite
primers, only if any), the drills, the games, and the **Assignment check** at the end
zone. Each stop is a DrillCard. When the check is passed: a "Done! Coach Ray will see
it" (Kid) / "Sent to Coach Ray." (Adult) confirmation, and a preset reaction row
("Got it!", "Need help", thumbs up) the learner can send. No text field.

### 9.13 Team playbook
For team-linked learners. A grid of play cards (name, small diagram thumbnail,
"Your job" line), filtered to the learner's positions, with a chip row for "All /
Offense / Defense / Special teams". A play opens full-screen: the diagram with the
learner's position ringed, **Animate** (film mode), the job text, and **Quiz me**.

### 9.14 Playbook map
The concept graph (spec 4.6) as a vertical skill tree per side (Offense, Defense,
Special teams, Rules), MasteryNodes joined by chalk lines to their prerequisites,
pinch-zoomable, with a search field and a list-mode alternative for TalkBack. Tapping a
node opens its glossary sheet with **Quiz me**.

## 10. Motion, haptics, sound

- Motion teaches or confirms; it never decorates. A **correct** answer fills its
  border and ticks the DriveBar forward 150ms. A **retry** does a single 6dp
  horizontal settle. The reveal (7.7) is the one long animation.
- Durations: micro 120ms, standard 220ms, reveal 900ms (Kid) / 630ms (Adult). Easing:
  standard `cubic-bezier(0.2, 0, 0, 1)`.
- **Reduced motion** (system animator scale 0, or the Settings toggle): all spring and
  slide become 150ms cross-fades; no confetti; the reveal cross-fades.
- Haptics: a light tick on select, a double tick on correct, a single soft thud on
  retry, a firm pulse on level pass. Off with a setting.
- Sound, short mono WAV/OGG under 120KB total per cue: select, correct, retry, level
  pass, star, timer tick, whistle, sticker. Kid default on, Adult default off. Never autoplay
  when another app holds audio focus. All sounds are original.
- **Recorded voice** (Little Huddle, spec 6.9): one warm, unhurried adult voice for
  every Level 0 prompt, picture name and feedback line; normalised loudness so a
  clip never startles; plays even with "Sound" off (voice is a separate setting,
  "Reading aloud").

## 11. Voice and copy

Feedback language is a design asset and is kept in `strings` JSON, not code.

| Moment | Kid | Adult |
|---|---|---|
| Right | "Got it!" / "Nice read!" / "You nailed it!" | "Correct." / "Right read." |
| Right, streak of 3 | "Three in a row!" | "Three straight." |
| Not quite | "Not quite. Here's why." | "Not quite." |
| Not quite, second time | "That one is tricky. Let's look together." | "Review this one." |
| Partial credit | "Almost! You got some of it." | "Partly right." |
| Game Day: best | "Great call, Coach!" | "Best call." |
| Game Day: OK | "Reasonable. There was a better one." | "Reasonable." |
| Game Day: poor | "That one cost you. See why." | "Costly." |
| Level pass | "Touchdown! Level 2 done!" | "Level 2 complete." |
| Streak lost | "Start a new streak today!" | "Streak reset." |
| Placement start | "Show me what you know!" | "Find your level." |
| New assignment | "Coach Ray picked something for you!" | "New assignment from Coach Ray." |
| Assignment done | "Done! Coach Ray will see it." | "Sent to Coach Ray." |
| Kudos received | "Coach Ray sent you a high five!" | "Kudos from Coach Ray." |
| Locked paid level | "Ask a grown-up about this one." | "Available with a Family or Team plan. Managed in the Coach app." |

**Little Huddle** lines are a separate set, spoken, four to eight words each:
"Tap the ball!", "Which team has the ball?", "Yes! That's a touchdown!",
"Let's try that one together.", "You got a sticker!"

Rules: never "wrong", "fail", "bad" or "incorrect" in Kid. Never blame ("you
missed"); say what the picture shows ("The safety drops down"). Never exclaim in Adult
except in milestones. Every explanation in a graded step answers *why*, in two
sentences at most in Kid.

## 12. Accessibility (non-optional)

- **Screen reader:** every screen reads in order; every diagram has a text
  description (7.6); the DriveBar announces "Step 4 of 9"; AnswerButtons announce
  "Option B, Cover 3, not selected"; results announce "Correct" or "Not quite" before
  the explanation.
- **Font scale:** layouts are tested at 100%, 130%, 160% and 200%. At 130%+ answer
  buttons grow vertically; the diagram never shrinks below 220dp tall and the card
  scrolls instead; HUDs wrap to two lines.
- **Colour:** never the only carrier (section 1.2). Retry is amber with a dash icon,
  not red.
- **Motor:** 48dp / 56dp targets, no required drags (every drag has a tap
  alternative), no time pressure in lessons or games except the Speed round, which
  has an **Untimed** option (grown-up zone or per profile) that keeps the scoring but
  removes the clock.
- **Cognitive:** one idea per card, consistent placement of Continue, reading aloud
  with word highlighting in Kid, glossary on any underlined term, no dark patterns (no
  streak guilt, no fake urgency, no forced ads).
- **Hearing:** all information is also visual; a sound is never the only cue. The one
  exception is Little Huddle, where the spoken prompt is primary for pre-readers;
  every Level 0 step also has a caption toggle in the grown-up zone and pictures that
  make the task clear without sound wherever possible.
- **Data views (Coach app):** every chart and heat map has a table alternative;
  mastery cells carry the state glyph (8.1), not colour alone.
- **Dyslexia-friendly:** Atkinson Hyperlegible, left-aligned ragged-right, line height
  1.4, optional extra letter spacing in Settings.
- **Contrast:** every text pair at least 4.5:1 (section 3); diagram paths at least 3:1 on both
  turf stripes (section 3.1). Verified by a scripted check in CI over the tokens.

## 13. Design gallery

A hidden **Design gallery** screen (Settings, long-press the version) renders every
component in both skins, Little Huddle mode, and both themes at 100% and 200% font,
plus every `PathKind`, `Side`, every step type, the five mastery states, and a sample
of coverage diagrams. The Coach app has the same gallery for its components
(section 14). It is the first place to look when a screen looks wrong, and the
target of the screenshot tests (spec section 13.4).

## 14. Coach & Parent app

The Coach app (spec 7) is for adults only. It uses the **Adult skin** ("Film Room")
with every token, type style and FieldDiagram unchanged, so a play drawn by a coach
looks exactly as it will on a player's phone. Clip appears only on the empty states.
The default theme is Floodlight; it follows the phone like the learner app.

### 14.1 Layout
- **Phone:** bottom navigation with four tabs: **Players**, **Assign**,
  **Playbook**, **More** (reports, account, plans, settings).
- **Tablet and large screens:** a navigation rail and a list-detail layout (roster on
  the left, the selected learner or team on the right). The play editor wants a
  tablet or a phone in landscape.
- Density: 48dp targets, 16sp body, tables use 14sp `label` style with tabular figures.
- A parent with no team sees **Players** as "My kids" and no Playbook tab.

### 14.2 Players
A list of linked learners and teams. A learner row: avatar, nickname, level, streak,
"last active", and a status pill for assignments ("On track", "Behind", "Done";
with icons). A team row opens the team view.

**Learner card:** header (avatar, nickname, level node, streak, minutes this week);
**Assignments** (AssignmentCards, coach-side); **Mastery** (a strip of MasteryNodes
for the level's concepts, plus "3 to work on" each with a one-tap **Assign practice**);
**Activity** (a day-by-day timeline). Parents also see **Time limits** and
**Linked coaches** (approve, revoke).

### 14.3 Team view and heat map
A sortable roster table (nickname, positions, level, last active, assignment %,
readiness per assigned pack). Above it, the **heat map**: learners as rows, concepts
of the selected pack as columns, each cell the mastery state glyph on a tint of its
state colour (8.1). Tapping a column header shows the concept's most-missed items,
with the actual question and the most common wrong answer, so the coach can teach
it at practice. A **Table** toggle shows the same data as plain numbers. Filters:
position group, group, pack.

### 14.4 Assign
A four-step sheet with a step indicator: **What** (search box, Packs grid filtered by
side and level, browse the catalogue, or pick team plays) -> **Who** (team, group,
individuals, with avatars) -> **Options** (due date, check score slider defaulting to
80%, "Include warm-ups for missing basics" switch, note with a 200-character counter)
-> **Preview** (per-learner plan length, e.g. "Leo: 6 drills + check, about 35
min"; warnings in `retry` with an icon when an item is two or more levels above a
learner, or has no kid version). The one primary is **Send**. A sent assignment is
read-only; **Edit** makes a new version.

### 14.5 Playbook editor
- Canvas: FieldDiagram at full size in editing mode, with the formation template
  picker at the top and a tool rail: **Select/Move**, **Path** (with a kind picker:
  pass, run, block, motion, blitz, drop, rotate), **Zone**, **Label**, **Undo/Redo**.
- Drawing and placing use section 7.9; a long-press on a player opens its sheet:
  label, position, and the **Job** text.
- A right panel (bottom sheet on phones): play name, concept tags (search the
  catalogue), and **Generated questions** listed with a toggle and edit for each.
- **Preview as player** shows the learner's play screen (9.13) for any position.
- Publish validates the play; problems list in the panel with "Show me" links that
  select the offending player or path.

### 14.6 Linking and consent
Plain, step-by-step, one decision per screen. The consent screen states in a short
bulleted list what is shared, who sees it, how to delete it, then the verification
step (method per spec 5.4). The link code shows large in tabular figures, with the QR
code above and a 10-minute expiry shown as text, not a countdown ring.

### 14.7 Reports and plans
Reports: the weekly summary as cards plus **Share PDF**. Plans: the current plan,
what it unlocks, and the store purchase flow; this is the only place prices appear in
either app.

### 14.8 Copy
Adult voice (section 11), written for parents who are not football experts: every
football term on a coach screen is a GlossaryChip; status words are "On track",
"Behind", "Done", never "Failing".
