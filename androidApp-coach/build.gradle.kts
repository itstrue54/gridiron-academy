import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// The Android Coach & Parent app (SPEC 1.2, 7): a thin shell around :shared:ui's
// CoachApp. Adults only; a separate listing from the learner app (ADR-001).

// The Supabase project (ADR-004) comes from local.properties, which is never committed:
//   supabase.url=https://<project>.supabase.co
//   supabase.anonKey=<the project's anon public key>
// Without them the app runs on the built-in demo account. The anon key is public by
// design; row-level security protects the data. Never put the service-role key here.
val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
fun quoted(key: String) = "\"" + (localProps.getProperty(key) ?: "").replace("\"", "") + "\""

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
        buildConfigField("String", "SUPABASE_URL", quoted("supabase.url"))
        buildConfigField("String", "SUPABASE_ANON_KEY", quoted("supabase.anonKey"))
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            buildConfigField("boolean", "REVIEWED_ONLY", "false")
        }
        release {
            // SPEC 13.3: a release ships only coach-signed levels. On by default; an internal
            // test build that needs unreviewed content passes -Pgridiron.reviewedOnly=false.
            buildConfigField("boolean", "REVIEWED_ONLY", (findProperty("gridiron.reviewedOnly") ?: "true").toString())
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
    implementation(project(":shared:data-supabase"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)
}
