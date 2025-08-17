import buildLogic.addTestBalloonPluginFromProject

plugins {
    id("buildLogic.android-application")
    // id("de.infix.testBalloon") version "$testBalloonVersion"  // required for TestBalloon outside this project
    alias(libs.plugins.org.jetbrains.kotlin.atomicfu)
}

// The following invocation supplements the TestBalloon plugin declaration inside this project:
addTestBalloonPluginFromProject(projects.testBalloonCompilerPlugin, projects.testBalloonFrameworkAbstractions)

android {
    namespace = "org.example.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

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
    // required for local tests with TestBalloon outside this project:
    //     implementation("de.infix.testBalloon:testBalloon-framework-core-jvm:${testBalloonVersion}")
    // instead of this project-internal dependency:
    testImplementation(project(projects.testBalloonFrameworkCore.path, "jvmRuntimeElements"))

    // required for instrumented tests with TestBalloon outside this project:
    //     implementation("de.infix.testBalloon:testBalloon-framework-core:${testBalloonVersion}")
    // instead of this project-internal dependency:
    androidTestImplementation(projects.testBalloonFrameworkCore)
    androidTestImplementation(libs.androidx.test.runner)

    androidTestImplementation(libs.org.jetbrains.kotlinx.atomicfu)
}
