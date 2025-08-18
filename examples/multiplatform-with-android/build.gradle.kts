import buildLogic.addTestBalloonPluginFromProject

plugins {
    id("buildLogic.multiplatform-plus-android-application")
    // id("de.infix.testBalloon") version "$testBalloonVersion"  // required for TestBalloon outside this project
}

// The following invocation supplements the TestBalloon plugin declaration inside this project:
addTestBalloonPluginFromProject(projects.testBalloonCompilerPlugin, projects.testBalloonFrameworkAbstractions)

kotlin {
    compilerOptions {
        // freeCompilerArgs.addAll("-P", "plugin:de.infix.testBalloon:debugLevel=DISCOVERY")
    }

    androidTarget()
    jvm()

    sourceSets {
        androidUnitTest {
            dependencies {
                // required for local tests with TestBalloon outside this project:
                //     implementation("de.infix.testBalloon:testBalloon-framework-core-jvm:${testBalloonVersion}")
                // instead of this project-internal dependency:
                implementation(project(projects.testBalloonFrameworkCore.path, "jvmRuntimeElements"))
            }
        }

        androidInstrumentedTest {
            dependencies {
                // required for instrumented tests with TestBalloon outside this project:
                //     implementation("de.infix.testBalloon:testBalloon-framework-core:${testBalloonVersion}")
                // instead of this project-internal dependency:
                implementation(projects.testBalloonFrameworkCore)
                implementation(libs.androidx.test.runner)
            }
        }
    }
}

android {
    namespace = "org.example.android.multiplatform"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.example.android.multiplatform"
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1,LICENSE*.md}"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}
