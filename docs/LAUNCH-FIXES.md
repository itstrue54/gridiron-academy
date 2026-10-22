# Launch fixes

From the pre-launch expert review of the learner release APK (main `6281dbd`, 2026-10-21):
UX, accessibility, performance, security, Play compliance, copy, a high school coach,
children's privacy, release engineering and kid learning design. Items are numbered as in
that review. Each phase is one PR; a phase is done when its tests pass and the APK checks
at the end of it hold.

Who decides: items marked **(owner)** need a decision or an account only the owner has.

## Phase 1. Content correctness

The football and the words a learner sees. Every edited step loses its sign-off (its
fingerprint changes) and goes back through `tools/review.py`.

- [x] **Feedback that contradicts the verdict (6).** Strip leading "Yes", "Right",
      "Correct", "Not quite", "No" from every `explanation`; the headline already gives
      the verdict. Validator rule: an explanation may not open with a verdict word.
- [x] **Wrong diagrams (7).**
  - [x] `c5-pressure-04/05`: six rush, so man coverage must account for the slot (H).
  - [x] `c5-pressure-01/06`: fire zone needs a curl-flat player on each side and a hook.
  - [x] `v4-gap-02/05`: power hits between the last down block and the kick-out.
  - [x] `v4-zone-03`: "sealed" means bounce; cut back when the edge is set hard.
  - [x] `p6-quarters-05`: the Cover 6 tell is the other safety, not corner depth alone.
- [x] **Man-on-the-back arrows** stop at 3-4 yards instead of running into the backfield,
      so they don't read as blitzes.
- [x] **Youth and high school rules (9).** Label NFL-only facts "In the NFL" and add the
      HS/youth line where kids will meet it: extra-point values in youth leagues, number
      eligibility (50-79) in HS, illegal contact (NFL-only, move out of Playing Safe),
      two-minute warning, overtime, down by contact, onside "unless the receiving team
      touches it", clock stops out of bounds. Fix the glossary play clock (NFHS 40/25
      since 2019).
- [x] **Coach's minor wording:** safety (score) "the ball carrier"; "forward pass";
      "aren't eligible"; route tree "higher is usually deeper"; red-zone four verticals;
      94% extra point to about 95%; overload tag and the simulated-pressure definition.
- [x] **Copy (20):** UK spellings, "Time out", "out-numbers", stray space, blame lines,
      "So close!" only when close.

Phase 1 status (2026-10-22): done in the `launch-fixes-content` PR. 110 edited steps lost
their sign-off; phase 6 edits and the coach read brought the queue to 167. itstrue54 signed
all 167 on 2026-10-22, so every level ships again.

## Phase 2. A learner app that matches its privacy promise

- [ ] **No network in the learner app (1).** Move Supabase/Ktor/coach UI out of what the
      learner app depends on; manifest guard for INTERNET; a CI check that the learner
      APK declares no INTERNET permission. ADR + SPEC.
- [ ] **R8 on (13)** with keep rules, a smoke test of every step type, save and restore.
- [ ] **Packaging (13):** fonts once, `localeFilters`, preview content to test resources.
- [ ] **Review gate (14):** on by default for release, fingerprints checked, CI builds
      `bundleRelease`.

## Phase 3. Kids can finish a level

- [x] **Speed round (5):** Untimed option (default for ages 8-9 and when TalkBack is on),
      a rules screen with Start, clock paused under the Leave dialog, the dialog cleared
      when the screen changes.
- [x] **Home (8):** name on one line (the streak chip moved under it), Home opens scrolled
      to the level being played. Order stays as designed: the kid drive map runs up the
      field (Level 1 at the goal line), the adult season list runs down; with the scroll
      the child no longer starts at Level 8.
- [x] **Checkpoint copy** (17): "so close" vs "not yet" wording landed in phase 1.
- [ ] **Kid pass mark** (owner): the Checkpoint is 80% (10 of 12) on every track. Lowering it
      for kids is a curriculum change, so it waits for your call.

## Phase 4. Accessibility

- [x] **Diagram list mode (4):** every tap target as a selectable row; default with
      TalkBack; graded through the same ids. Checked on the phone.
- [x] **Announce results (11):** live region on the verdict; "Correct"/"Not quite" in
      answer labels; Match and Order readable by ear.
- [x] **Not colour alone (12):** selected answers show a filled dot; selected and graded
      zones get a corner badge (dot, check, dash).
- [x] **200% text pass** with render tests at font scale 2 (`f2-*`). Fixed: level titles
      broke mid-word on Home, Match columns split words, letters and numbers ran into
      Match and Order text.
- [ ] **Read-aloud (11) (owner):** build text-to-speech for the kid skin, or narrow the age
      claim for launch.

## Phase 5. Store and policy (owner)

- [ ] Upload keystore and Play App Signing; `signingConfig` from a file outside the repo (2).
- [ ] Privacy policy, hosted, linked in the grown-up zone and on the listing (3).
- [ ] Feature graphic, screenshots, listing text with "not affiliated with the NFL",
      Families and Data safety forms, content rating, licences screen (21).

## Phase 6. Robustness and polish

- [x] Startup: no validation in release, splash until loaded (15).
- [x] Save encoded off the main thread, attempt log capped (newest 20,000), import read off
      the main thread with an 8 MB limit (15).
- [x] Grown-up gate: two-digit times one-digit in words, a one-minute lock after three
      misses, the zone closes when the app leaves the screen (not for the backup picker) (16).
- [x] Question quality (17): 24 true/false items rewritten as false (69:18 became 45:42;
      the validator now fails a lopsided level); 32 adult and shared questions get real
      wrong answers instead of throwaways (answer clearly longest: 50 became 19 of 208).
- [x] Pronouns (18): kid levels and kid glossary say "they". NFL levels keep "he".
- [ ] Flag-football scoping (18): which rules differ in flag; needs the owner's call on
      which flag rulebook (NFL FLAG, league rules) the kid course speaks for.
- [x] Status bar on the dark skin (19), plurals (20). Status bar not yet seen on the phone.
- [ ] UX (19): feedback sheet covering the diagram, larger tap targets on small dots,
      screen state kept across process death, map back and zoom buttons; term consistency
      (20).
- [ ] Safety and concussion card; a kid bridge unit before Varsity-4 (10).

## Before sync (not this release)

Server-written consent after the VPC check; no under-13 own accounts; column-limited
learner updates; delete, export, revoke and retention; filtered coach names and team
names; per-assistant visibility; validated event payloads; helper functions not callable
over RPC.
