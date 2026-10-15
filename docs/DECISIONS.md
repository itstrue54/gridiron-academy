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
