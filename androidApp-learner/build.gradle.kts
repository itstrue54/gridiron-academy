plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// The Android learner app (SPEC 1.2): a thin shell around :shared:ui. It supplies the
// content source, the save file, the clock, back handling and system settings.

android {
    namespace = "io.github.itstrue54.gridironacademy.learner"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        // Permanent once published to Google Play.
        applicationId = "io.github.itstrue54.gridironacademy"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        debug {
            // A development build installs beside the store one, not over it.
            applicationIdSuffix = ".debug"
            buildConfigField("boolean", "REVIEWED_ONLY", "false")
        }
        release {
            // SPEC 13.3: ship only coach-signed levels. Off until the course is signed:
            // build with -Pgridiron.reviewedOnly=true to turn it on.
            buildConfigField("boolean", "REVIEWED_ONLY", (findProperty("gridiron.reviewedOnly") ?: "false").toString())
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
        buildConfig = true
    }
}

dependencies {
    implementation(project(":shared:ui"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.kotlinx.coroutines.android)
}
