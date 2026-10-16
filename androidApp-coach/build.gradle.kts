plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// The Android Coach & Parent app (SPEC 1.2, 7): a thin shell around :shared:ui's
// CoachApp. Adults only; a separate listing from the learner app (ADR-001).

android {
    namespace = "io.github.itstrue54.gridironacademy.coach"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        // Permanent once published to Google Play.
        applicationId = "io.github.itstrue54.gridironacademy.coach"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":shared:ui"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)
}
