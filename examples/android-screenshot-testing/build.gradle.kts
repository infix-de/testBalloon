import buildLogic.robolectricJdkVersion

plugins {
    id("buildLogic.android-application")
    id("de.infix.testBalloon")
    id("org.jetbrains.kotlin.plugin.compose")
    alias(libs.plugins.io.github.takahirom.roborazzi)
}

tapmoc {
    java(robolectricJdkVersion())
}

android {
    namespace = "org.example.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    buildFeatures {
        compose = true
    }

    defaultConfig {
        applicationId = "org.example.android"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        testOptions {
            animationsDisabled = true
            unitTests.isIncludeAndroidResources = true
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1,LICENSE*.md}"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
        }
    }
}

dependencies {
    // Specify the Compose BOM with a version definition
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    testImplementation(composeBom)

    // Material Design 3
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")

    // Compose Preview
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation(libs.junit.junit4)
    testImplementation("androidx.compose.ui:ui-test-junit4:1.10.0")
    testImplementation(projects.testBalloonIntegrationRobolectric)
    testImplementation(libs.io.github.takahirom.roborazzi)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.org.jetbrains.kotlin.test) // for assertions only

    // Needed for createComposeRule(), but not for createAndroidComposeRule<YourActivity>():
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
