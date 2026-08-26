import buildLogic.robolectricJdkVersion

plugins {
    id("buildLogic.kotlin-multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("de.infix.testBalloon")
}

tapmoc {
    java(robolectricJdkVersion())
}

kotlin {
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
                implementation("androidx.compose.ui:ui-test-junit4:1.10.0")
                implementation("androidx.compose.material3:material3:1.4.0")
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
