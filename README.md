# Gridiron Academy

Learn American football the way Duolingo teaches a language: short lessons, games and
tests on an animated field, from "what is a touchdown?" at age 6 to reading an NFL
coverage disguise before the snap. A separate **Coach & Parent** app lets the adults in a
learner's life see progress and assign the exact offense and defense their team runs.

Android first, built with Kotlin Multiplatform and Compose Multiplatform so iOS and a web
coach dashboard are a port, not a rewrite.

## What's here

| Path | What it is |
|---|---|
| `shared/core` | The engine: content model and validator, grading, lessons, gates, mastery, assignment planning. Pure common Kotlin, deterministic (seeded `Rng`, no clock). |
| `shared/core/src/commonMain/resources/content` | Every lesson, diagram, scheme pack and glossary term, as JSON. Eight levels, two tracks (kid and adult) that meet at Level 4. |
| `shared/data` | Repositories: the save format and its migrations, the coach dashboard, the Supabase client and a demo account. |
| `shared/ui` | Compose Multiplatform screens for both apps and the field renderer. |
| `androidApp-learner`, `androidApp-coach` | The two Android apps. |
| `backend` | Supabase schema and row-level security, with tests on PGlite. |
| `docs` | `SPEC.md` (the source of truth), `DESIGN.md`, `CURRICULUM.md`, decisions, content reviews. |

## Build and test

```bash
./gradlew :shared:core:jvmTest :shared:data:jvmTest :shared:ui:jvmTest
./gradlew :androidApp-learner:assembleDebug :androidApp-coach:assembleDebug
cd backend && npm ci && npm test
```

The Coach app runs on a built-in demo team unless a Supabase project is configured in
`local.properties` (see `backend/README.md`).

## License

Copyright © 2026 itstrue54. All rights reserved.

The source is public so it can be read. No license is granted to copy, modify or
distribute it, in whole or in part.

The bundled fonts (Atkinson Hyperlegible, Barlow Semi Condensed and Fredoka) are under
the SIL Open Font License 1.1; their license texts are in
[`docs/licenses/fonts/`](docs/licenses/fonts/).

Fictional players and teams; no NFL, team or player names, logos or licensed content.
