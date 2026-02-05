import buildLogic.addTestBalloonPluginFromProject
import buildLogic.versionFromCatalog
import tapmoc.Severity

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("com.gradleup.tapmoc")
    // id("de.infix.testBalloon") version "$testBalloonVersion"  // required for TestBalloon outside this project
}

// The following invocation supplements the TestBalloon plugin declaration inside this project:
addTestBalloonPluginFromProject(projects.testBalloonCompilerPlugin, projects.testBalloonFrameworkShared)

tapmoc {
    java(versionFromCatalog("jdk").toInt().coerceAtLeast(17)) // JUnit Jupiter requires JVM 17 or higher
    kotlin(versionFromCatalog("org.jetbrains.kotlin"))

    checkDependencies(Severity.ERROR)
}

kotlin {
    jvm()

    androidLibrary {
        namespace = "org.example.android.multiplatform.library"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withHostTestBuilder {}.configure {
            // isIncludeAndroidResources = true
        }
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            managedDevices {
                localDevices {
                    @Suppress("UnstableApiUsage")
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

    sourceSets {
        commonTest {
            dependencies {
                implementation(projects.testBalloonFrameworkCore)
                implementation(kotlin("test")) // for assertions only
            }
        }

        jvmTest {
            dependencies {
                implementation(libs.org.junit.jupiter.engine)
            }
        }

        named("androidHostTest") {
            dependencies {
                // required for host-side tests with TestBalloon outside this project:
                //     implementation("de.infix.testBalloon:testBalloon-framework-core:${testBalloonVersion}")
                // instead of this project-internal dependency:
                implementation(projects.testBalloonFrameworkCore)
                implementation(libs.junit.junit4)
                // required for host-side tests with TestBalloon outside this project:
                //     implementation("de.infix.testBalloon:testBalloon-integration-roboelectric:${testBalloonVersion}")
                // instead of this project-internal dependency:
                implementation(projects.testBalloonIntegrationRoboelectric)
                implementation(libs.androidx.test.core)
            }
        }

        named("androidDeviceTest") {
            dependencies {
                // required for device-side tests with TestBalloon outside this project:
                //     implementation("de.infix.testBalloon:testBalloon-framework-core:${testBalloonVersion}")
                // instead of this project-internal dependency:
                implementation(projects.testBalloonFrameworkCore)
                implementation(libs.androidx.test.runner)
            }
        }
    }
}
