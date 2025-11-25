import buildLogic.addTestBalloonPluginFromProject
import buildLogic.jsTargets
import buildLogic.nativeTargets
import buildLogic.versionFromCatalog

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("com.gradleup.compat.patrouille")
    id("org.jetbrains.kotlin.plugin.compose")
}

addTestBalloonPluginFromProject(projects.testBalloonCompilerPlugin, projects.testBalloonFrameworkShared)

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

    // @OptIn(ExperimentalWasmDsl::class)
    // wasmWasi {
    //     nodejs()
    // }
    //
    // @OptIn(ExperimentalKotlinGradlePluginApi::class)
    // applyHierarchy {
    //     withWasmWasi()
    // }

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
                        device = "Pixel 2"
                        apiLevel = 30
                        systemImageSource = "aosp"
                    }
                }
            }
        }
    }

    sourceSets {
        // Specify the Compose BOM with a version definition
        val composeBom = project.dependencies.platform(libs.androidx.compose.bom)

        commonTest {
            dependencies {
                implementation(projects.testBalloonFrameworkCore)
                implementation(kotlin("test")) // for assertions only
                implementation(libs.com.benwoodworth.parameterize)
            }
        }

        androidMain {
            dependencies {
                implementation(composeBom)
            }
        }

        jvmMain {
            dependencies {
                implementation(libs.org.jetbrains.kotlinx.coroutines.swing)
            }
        }

        named("androidHostTest") {
            dependencies {
                // required for local tests with TestBalloon outside this project:
                //     implementation("de.infix.testBalloon:testBalloon-framework-core:${testBalloonVersion}")
                // instead of this project-internal dependency:
                implementation(projects.testBalloonFrameworkCore)
                implementation(libs.junit.junit4)

                // The Compose compiler plugin requires a compose runtime to be present, even if not used here.
                implementation("androidx.compose.ui:ui-test-junit4")
            }
        }

        named("androidDeviceTest") {
            dependencies {
                // required for instrumented tests with TestBalloon outside this project:
                //     implementation("de.infix.testBalloon:testBalloon-framework-core:${testBalloonVersion}")
                // instead of this project-internal dependency:
                implementation(projects.testBalloonFrameworkCore)
                implementation(libs.androidx.test.runner)

                implementation(composeBom)

                // Material Design 3
                implementation("androidx.compose.material3:material3")

                // Test rules and transitive dependencies:
                implementation("androidx.compose.ui:ui-test-junit4")
                // Needed for createComposeRule(), but not for createAndroidComposeRule<YourActivity>():
                implementation("androidx.compose.ui:ui-test-manifest")
            }
        }
    }
}
