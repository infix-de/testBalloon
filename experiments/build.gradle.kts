import buildLogic.addTestBalloonPluginFromProject
import buildLogic.kotlinVersion
import buildLogic.robolectricJdkVersion
import org.jetbrains.kotlin.gradle.ExperimentalJsTestDsl
import tapmoc.Severity
import java.time.Duration

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("com.gradleup.tapmoc")
}

addTestBalloonPluginFromProject(projects.testBalloonCompilerPlugin, projects.testBalloonFrameworkShared)

tapmoc {
    java(robolectricJdkVersion())
    kotlin(kotlinVersion())

    checkDependencies(Severity.ERROR)
}

kotlin {
    js {
        browser {
            @OptIn(ExperimentalJsTestDsl::class)
            // Add and configure the new test{} block
            test {
                // Set up options common for all browsers
                browserDefaults {
                    timeout = Duration.ofSeconds(2)
                    headless = true
                }
                // Enable Chromium test runner
                chromium {
                    // Override the common timeout option
                    timeout = Duration.ofSeconds(5)
                    launchArgs.add("--no-sandbox")
                }
            }
        }
    }

    jvm()

    androidLibrary {
        namespace = "org.example.android.multiplatform.library"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withHostTestBuilder {}.configure {
            isIncludeAndroidResources = true
        }
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            // instrumentationRunnerArguments["TESTBALLOON_REPORTING_PATH_LIMIT_BELOW_TOP_LEVEL"] = "138"
            managedDevices {
                localDevices {
                    @Suppress("UnstableApiUsage")
                    create("pixel2api30") {
                        device = "Pixel 2"
                        apiLevel = 30
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
                implementation(libs.org.jetbrains.kotlin.test) // for assertions only
            }
        }

        named("androidHostTest") {
            dependencies {
                // required for host-side tests with TestBalloon outside this project:
                //     implementation("de.infix.testBalloon:testBalloon-integration-robolectric:${testBalloonVersion}")
                // instead of this project-internal dependency:
                implementation(projects.testBalloonIntegrationRobolectric)
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
