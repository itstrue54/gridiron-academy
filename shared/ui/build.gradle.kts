plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

// Shared Compose Multiplatform UI: the design system (docs/DESIGN.md) and every
// screen component. Platform services (TTS, audio, billing) stay in the app shells.
// jvm (desktop) is for tests: it renders components headlessly to PNG.

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    android {
        namespace = "io.github.itstrue54.gridironacademy.ui"
        compileSdk = 37
        minSdk = 26
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    // Declared so commonMain is compiled against common APIs only (ADR-003).
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":shared:core"))
            api(compose.runtime)
            api(compose.foundation)
            api(compose.material3)
            api(compose.ui)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        jvmTest.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(project.dependencies.platform(libs.jupiter.bom))
            runtimeOnly("org.junit.platform:junit-platform-launcher")
        }
    }
}

listOf("jvmTest", "check").forEach { name ->
    tasks.named(name) { dependsOn("compileCommonMainKotlinMetadata") }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
    // Headless renders get a one-folder fontconfig. Skia's default font manager scans
    // every system font at startup, which stalled for 10+ minutes on a machine with ~2,800
    // fonts, and a fixed font set keeps PNGs identical across machines (SPEC 13.4).
    // DejaVu ships with Ubuntu and most Linux CI images; the brand fonts replace it once
    // they are bundled (DESIGN 4).
    val fontsConf = layout.buildDirectory.file("test-fonts/fonts.conf")
    val fontDir = "/usr/share/fonts/truetype/dejavu"
    outputs.file(fontsConf)
    doFirst {
        if (File(fontDir).isDirectory) {
            fontsConf.get().asFile.apply {
                parentFile.mkdirs()
                writeText(
                    """<?xml version="1.0"?><!DOCTYPE fontconfig SYSTEM "fonts.dtd">
                    |<fontconfig><dir>$fontDir</dir><cachedir>${parentFile.absolutePath}/cache</cachedir></fontconfig>
                    |""".trimMargin(),
                )
            }
            environment("FONTCONFIG_FILE", fontsConf.get().asFile.absolutePath)
        } else {
            logger.warn("$fontDir not found: render tests use the system fonts and may stall for minutes while Skia scans them. Install DejaVu (fonts-dejavu-core).")
        }
    }
}
