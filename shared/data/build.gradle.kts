plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.serialization)
}

// Repositories and (later) sync (SPEC 11.1): apps -> ui -> data -> core. Common Kotlin
// only, and no network: the Supabase client lives in :shared:data-supabase, which only
// the Coach app depends on (ADR-012), so the learner app stays offline (SPEC 5.4).

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    android {
        namespace = "io.github.itstrue54.gridironacademy.data"
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
            api(libs.kotlinx.coroutines.core)
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

listOf("jvmTest", "check").forEach { name ->
    tasks.named(name) { dependsOn("compileCommonMainKotlinMetadata") }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
}
