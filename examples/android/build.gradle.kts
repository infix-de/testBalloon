import buildLogic.addTestBalloonPluginFromProject

plugins {
    id("buildLogic.android-application")
    // id("de.infix.testBalloon") version "$testBalloonVersion"  // required for TestBalloon outside this project
    id("org.jetbrains.kotlin.plugin.atomicfu")
    id("org.jetbrains.kotlin.plugin.compose")
}

// The following invocation supplements the TestBalloon plugin declaration inside this project:
addTestBalloonPluginFromProject(projects.testBalloonCompilerPlugin, projects.testBalloonFrameworkShared)

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

        @Suppress("UnstableApiUsage")
        testOptions {
            animationsDisabled = true
            managedDevices {
                localDevices {
                    create("pixel2api30") {
                        // Use device profiles you typically see in Android Studio.
                        device = "Pixel 2"
                        // Use only API levels 27 and higher.
                        apiLevel = 30
                        // To include Google services, use "google".
                        systemImageSource = "aosp"
                    }
                }
            }
        }
    }

    kotlin {
        compilerOptions {
            // freeCompilerArgs.addAll("-P", "plugin:de.infix.testBalloon:debugLevel=DISCOVERY")
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
    androidTestImplementation(composeBom)

    // Material Design 3
    implementation("androidx.compose.material3:material3")

    // required for host-side tests with TestBalloon outside this project:
    //     testImplementation("de.infix.testBalloon:testBalloon-framework-core:${testBalloonVersion}")
    // instead of this project-internal dependency:
    testImplementation(projects.testBalloonFrameworkCore)
    testImplementation(libs.junit.junit4)

    // required for device-side tests with TestBalloon outside this project:
    //     implementation("de.infix.testBalloon:testBalloon-framework-core:${testBalloonVersion}")
    // instead of this project-internal dependency:
    androidTestImplementation(projects.testBalloonFrameworkCore)
    androidTestImplementation(libs.androidx.test.runner)

    androidTestImplementation(libs.org.jetbrains.kotlinx.atomicfu)

    // Test rules and transitive dependencies:
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    // Needed for createComposeRule(), but not for createAndroidComposeRule<YourActivity>():
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
