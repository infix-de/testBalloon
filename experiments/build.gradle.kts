import buildLogic.addTestBalloonPluginFromProject
import buildLogic.versionFromCatalog
import tapmoc.Severity

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("com.gradleup.tapmoc")
}

addTestBalloonPluginFromProject(projects.testBalloonCompilerPlugin, projects.testBalloonFrameworkShared)

tapmoc {
    java(versionFromCatalog("jdk").toInt())
    kotlin(versionFromCatalog("org.jetbrains.kotlin"))

    checkDependencies(Severity.ERROR)
}

kotlin {
    compilerOptions {
        // freeCompilerArgs.addAll("-P", "plugin:de.infix.testBalloon:debugLevel=DISCOVERY")
    }

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
                implementation(kotlin("test")) // for assertions only
            }
        }

        named("androidHostTest") {
            dependencies {
                // required for host-side tests with TestBalloon outside this project:
                //     implementation("de.infix.testBalloon:testBalloon-framework-core:${testBalloonVersion}")
                // instead of this project-internal dependency:
                implementation(projects.testBalloonFrameworkCore)
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
