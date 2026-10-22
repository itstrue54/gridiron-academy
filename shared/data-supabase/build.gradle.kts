plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.serialization)
}

// The Supabase backend client (ADR-012): only the Coach app depends on this module, so the
// learner app ships no network stack and declares no INTERNET permission (SPEC 5.4).

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    android {
        namespace = "io.github.itstrue54.gridironacademy.data.supabase"
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
            api(project(":shared:data"))
            implementation(project.dependencies.platform(libs.supabase.bom))
            implementation(libs.supabase.postgrest)
            implementation(libs.supabase.auth)
        }
        // supabase-kt finds its HTTP engine per platform.
        jvmMain.dependencies { implementation(libs.ktor.client.okhttp) }
        androidMain.dependencies { implementation(libs.ktor.client.okhttp) }
        iosMain.dependencies { implementation(libs.ktor.client.darwin) }
    }
}
