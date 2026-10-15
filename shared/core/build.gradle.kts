plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.serialization)
}

// Pure common Kotlin: no platform APIs in commonMain, ever (docs/SPEC.md section 11.5).
// JVM is for tests and tools; Android is the first app target; iOS joins at M5.

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    android {
        namespace = "io.github.itstrue54.gridironacademy.core"
        compileSdk = 37
        minSdk = 26
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    // Declared so commonMain is compiled on its own against the common stdlib: with
    // only JVM-family targets Kotlin skips that step and java.* would leak into common
    // code. They cannot link on Linux; Gradle skips them there until M5 adds a Mac CI.
    iosArm64()
    iosSimulatorArm64()

    // jvm and android share one JVM source set so both read the content JSON that is
    // packaged as Java resources; there is no second copy in app assets.
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
        common {
            group("jvmShared") {
                withJvm()
                withCompilations { it.target.platformType == org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.androidJvm }
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        jvmTest.dependencies {
            implementation(project.dependencies.platform(libs.jupiter.bom))
            runtimeOnly("org.junit.platform:junit-platform-launcher")
        }
    }
}

// Determinism (SPEC 11.5 rule 2): randomness comes from the seeded Rng, time and ids
// are passed in. The compiler can't see these because they are legal common Kotlin.
val checkDeterminism by tasks.registering {
    val sources = fileTree("src/commonMain/kotlin")
    val stamp = layout.buildDirectory.file("checkDeterminism/ok")
    inputs.files(sources)
    outputs.file(stamp)
    doLast {
        val banned = Regex("""\bRandom\b|kotlin\.random|Clock\.System|TimeSource|Uuid\.random""")
        val hits = sources.files.flatMap { f ->
            f.readLines().mapIndexedNotNull { i, line ->
                // Comments may talk about randomness; only code is checked.
                val code = line.substringBefore("//").trim()
                if (code.startsWith("*") || code.startsWith("/*")) null
                else if (banned.containsMatchIn(code)) "${f.name}:${i + 1}: ${line.trim()}" else null
            }
        }
        if (hits.isNotEmpty()) {
            throw GradleException("Non-deterministic API in commonMain; use Rng or pass time in:\n" + hits.joinToString("\n"))
        }
        stamp.get().asFile.apply { parentFile.mkdirs(); writeText("ok") }
    }
}

// The common-only compile proves commonMain uses no java.*; together with
// checkDeterminism it runs with the usual test command and with every `check`.
listOf("jvmTest", "check").forEach { name ->
    tasks.named(name) { dependsOn("compileCommonMainKotlinMetadata", checkDeterminism) }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
}
