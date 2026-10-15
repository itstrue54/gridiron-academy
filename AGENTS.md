# House rules

Read `docs/SPEC.md` first.

1. `:shared:core` is pure common Kotlin (KMP). No `android.*`, `java.*` or `javax.*` in `commonMain`; platform code goes in `jvmMain`/`androidMain` or the app shells.
2. No `Math.random`, `java.util.Random`, `System.currentTimeMillis`, `System.nanoTime`, `UUID.randomUUID`, `kotlin.random` or `Clock.System` in `:shared:core`. Use the injected `Rng` and pass time in. (`checkDeterminism` enforces this.)
3. Curriculum, glossary and diagrams are JSON under `shared/core/src/commonMain/resources/content`. Never put lesson text in Kotlin.
4. Every graded step has an explanation. Every concept id must exist in `glossary.json`.
5. Models are immutable `data class`es with `val` only.
6. Run `./gradlew :shared:core:jvmTest` (it also compiles commonMain on its own and runs `checkDeterminism`) before proposing a change. Never weaken or delete a failing test; report it.
7. Football accuracy matters more than volume. When unsure of a rule or coverage detail, mark it for review instead of guessing; kid-facing text must be right.
8. Update `docs/SPEC.md` and `docs/CURRICULUM.md` in the same change as the behavior or content they describe.
9. Commit prefixes: `feat:` `fix:` `test:` `docs:` `content:` `refactor:` `chore:`.
