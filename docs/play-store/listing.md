# Play Store listing: Gridiron Academy (learner app)

Draft for the owner to paste into Play Console. Launch scope per ADR-015: learner app only,
ages 6 and up. Fields marked {{...}} need the owner.

## Main store listing

**App name** (30 max): Gridiron Academy

**Short description** (80 max):
Learn football step by step, from your first touchdown to reading the defense.

**Full description** (4000 max):

Football makes a lot more sense when someone explains it. Gridiron Academy teaches the game
in short lessons, a few minutes at a time, with plays drawn on a field you can zoom into.

Start with the basics: the field, downs, scoring and who plays where. Then keep climbing,
eight levels in all, through penalties and the clock, formations and personnel, run
schemes, pass routes and coverages, and finally the disguises coordinators use to fool a
quarterback before the snap.

FOR KIDS AND GROWN-UPS
• A kid course with bright, friendly lessons, and a grown-up course that moves faster. They
  meet at Level 4 and climb the rest together.
• Short drills: learn a card, then answer a few questions about it.
• Tap the field: find the open gap, the deep safety or the receiver who's covered.
• Each level ends with a Checkpoint and a game, like a timed speed round or reading a
  defense before the snap.
• Practice brings back what you missed, and a streak keeps you coming back.
• Flag football rules, explained where they differ from tackle.

BUILT FOR FAMILIES
• No ads. No accounts. No in-app purchases.
• The app collects no personal information and can't reach the internet. Progress stays on
  your device.
• A grown-up zone, behind a check, holds settings, a progress report and backups.
• Large text, screen reader support and a list view for every field diagram.

Gridiron Academy is not affiliated with or endorsed by the National Football League, NFL
FLAG or any team.

**App icon:** `assets/icon-512.png`
**Feature graphic:** `assets/feature-graphic.png` (1024x500, from `tools/make-feature-graphic.py`)
**Phone screenshots:** 4 to 8, from the release build on a phone (see below).

## Store settings

- **Category:** Education. **Tags:** Education, Sports.
- **Contact email:** {{CONTACT_EMAIL}}
- **Privacy policy:** https://itstrue54.github.io/gridiron-academy/privacy.html

## App content forms

- **Target audience and content:** ages 6-8, 9-12, 13-15, 16-17 and 18+. Children are among
  the audience, so the app joins the Families policy. The listing doesn't call it a kids-only app.
- **Ads:** no ads.
- **Data safety:** no data collected, no data shared. No permissions requested beyond the
  defaults; no INTERNET permission. Backups are files the user saves with the system file
  picker; the app never sends them.
- **Content rating (IARC):** Educational reference; no violence beyond describing tackling
  in a sport; no user-generated content, chat, location, purchases or gambling. Expect
  Everyone / PEGI 3.
- **Government apps, financial features, health:** none.
- **News app:** no.

## Phone screenshots

In `screenshots/`, taken on 2026-10-24 from the release build on a Galaxy S24 Ultra, with test
saves restored through the grown-up zone. Cropped to 1080x2111 (status and navigation bars
off), because Play rejects a side more than twice the other.

1. `1-kid-home`: kid Home, the drive map, at Level 4.
2. `2-kid-diagram-card`: a kid card with a field diagram (A, B and C gaps).
3. `3-tap-the-field`: a tap question after a right answer, the verdict sheet folded.
4. `4-adult-home`: adult Home in Prime Time.
5. `5-read-the-defense`: a Level 4 Read the Defense question.
6. `6-playbook-map`: the Playbook map with concepts mastered.
7. `7-progress-report`: the grown-up zone progress report.
