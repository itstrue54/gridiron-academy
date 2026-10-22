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

    // The app is English only; drop the 80-odd translations AndroidX and Compose bring.
    androidResources {
        localeFilters += "en"
    }

    packaging {
        resources {
            // Compose resources ship once, as assets; the Java-resource copy is unread.
            excludes += "composeResources/**"
            // Preview units are test fixtures (ContentTest, ScreenRenderTest), not course.
            excludes += "content/preview/**"
        }
    }

    buildTypes {
        debug {
            // A development build installs beside the store one, not over it.
            applicationIdSuffix = ".debug"
            buildConfigField("boolean", "REVIEWED_ONLY", "false")
        }
        release {
            // SPEC 13.3: a release ships only coach-signed levels. On by default; an internal
            // test build that needs unreviewed content passes -Pgridiron.reviewedOnly=false.
            buildConfigField("boolean", "REVIEWED_ONLY", (findProperty("gridiron.reviewedOnly") ?: "true").toString())
            // R8 shrinks code and resources (ADR-013). Library keep rules ship in their
            // jars (kotlinx.serialization, Compose); app-specific ones go in keep-rules.pro.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "keep-rules.pro")
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
    implementation(libs.kotlinx.coroutines.android)
}

// SPEC 5.4: an unlinked learner's data never leaves the device, so the learner app declares
// no INTERNET permission. A library that merges one in fails the build here (it happened:
// the Supabase client, ADR-012). When sync ships, remove this check on purpose.
abstract class NoInternetPermission : DefaultTask() {
    @get:InputFile abstract val manifest: RegularFileProperty

    @TaskAction fun check() {
        if ("android.permission.INTERNET" in manifest.get().asFile.readText()) {
            throw GradleException("The learner app must not declare INTERNET (SPEC 5.4): ${manifest.get().asFile}")
        }
    }
}

androidComponents {
    onVariants { variant ->
        val name = variant.name.replaceFirstChar { it.uppercase() }
        val check = tasks.register<NoInternetPermission>("check${name}NoInternet") {
            manifest.set(variant.artifacts.get(com.android.build.api.artifact.SingleArtifact.MERGED_MANIFEST))
        }
        tasks.matching { it.name == "assemble$name" || it.name == "bundle$name" }.configureEach { dependsOn(check) }
    }
}
