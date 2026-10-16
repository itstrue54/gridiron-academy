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
        // Compose resources (fonts, strings) must land in the APK's assets on Android;
        // without this the KMP library plugin packs them as Java resources and fonts
        // silently fall back to the system font.
        androidResources {
            enable = true
        }
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    // Declared so commonMain is compiled against common APIs only (ADR-003).
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":shared:data"))
            api(compose.runtime)
            api(compose.foundation)
            api(compose.material3)
            api(compose.ui)
            implementation(compose.components.resources)
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

compose.resources {
    packageOfResClass = "io.github.itstrue54.gridironacademy.ui.resources"
    publicResClass = false
}

listOf("jvmTest", "check").forEach { name ->
    tasks.named(name) { dependsOn("compileCommonMainKotlinMetadata") }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
    // Headless renders get a fontconfig that sees only the bundled fonts. Skia's default
    // font manager scans every system font at startup, which stalled for 10+ minutes on a
    // machine with ~2,800 fonts; a fixed font set also keeps PNGs identical everywhere.
    // FontFamily.Default (sans-serif) resolves to Atkinson so stock components render too.
    val fontsConf = layout.buildDirectory.file("test-fonts/fonts.conf")
    val fontDir = file("src/commonMain/composeResources/font").absolutePath
    outputs.file(fontsConf)
    doFirst {
        fontsConf.get().asFile.apply {
            parentFile.mkdirs()
            writeText(
                """<?xml version="1.0"?><!DOCTYPE fontconfig SYSTEM "fonts.dtd">
                |<fontconfig><dir>$fontDir</dir><cachedir>${parentFile.absolutePath}/cache</cachedir>
                |<alias><family>sans-serif</family><prefer><family>Atkinson Hyperlegible</family></prefer></alias>
                |</fontconfig>
                |""".trimMargin(),
            )
        }
        environment("FONTCONFIG_FILE", fontsConf.get().asFile.absolutePath)
    }
}
