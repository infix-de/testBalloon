import buildLogic.addTestBalloonPluginFromProject
import buildLogic.applyHierarchy
import buildLogic.jsTargets
import buildLogic.nativeTargets
import buildLogic.versionFromCatalog
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("com.gradleup.compat.patrouille")
}

addTestBalloonPluginFromProject(projects.testBalloonCompilerPlugin, projects.testBalloonFrameworkAbstractions)

compatPatrouille {
    java(versionFromCatalog("jdk").toInt())
    kotlin(versionFromCatalog("org.jetbrains.kotlin"))

    checkApiDependencies(compat.patrouille.Severity.ERROR)
    checkRuntimeDependencies(compat.patrouille.Severity.ERROR)
}

kotlin {
    compilerOptions {
        // freeCompilerArgs.addAll("-P", "plugin:de.infix.testBalloon:debugLevel=DISCOVERY")
    }

    jvm()
    jsTargets()
    nativeTargets()

    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi {
        nodejs()
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyHierarchy {
        withWasmWasi()
    }

    androidLibrary {
        namespace = "org.example.android.multiplatform.library"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withJava() // enable java compilation support
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

        named("androidHostTest") {
            dependencies {
                // WORKAROUND: See examples/multiplatform-library-with-android/README.md
                if (project.findProperty("local.androidHostTestDance") != "removeDependency") {
                    // required for local tests with TestBalloon outside this project:
                    //     implementation("de.infix.testBalloon:testBalloon-framework-core-jvm:${testBalloonVersion}")
                    // instead of this project-internal dependency:
                    implementation(project(projects.testBalloonFrameworkCore.path, "jvmRuntimeElements"))
                }
            }
        }

        named("androidDeviceTest") {
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
