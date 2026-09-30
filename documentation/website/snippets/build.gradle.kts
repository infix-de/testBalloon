import buildLogic.junitJupiterJdkVersion

plugins {
    id("buildLogic.kotlin-multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("de.infix.testBalloon")
    id("org.jetbrains.kotlin.plugin.compose")
}

tapmoc {
    java(junitJupiterJdkVersion())
}

kotlin {
    jvm()

    // @OptIn(ExperimentalWasmDsl::class)
    // wasmWasi {
    //     nodejs()
    // }
    //
    // @OptIn(ExperimentalKotlinGradlePluginApi::class)
    // applyHierarchy {
    //     withWasmWasi()
    // }

    android {
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
                implementation(libs.org.jetbrains.kotlin.test)
                implementation(libs.com.benwoodworth.parameterize)

                implementation(composeBom)

                // Material Design 3
                implementation("androidx.compose.material3:material3")
            }
        }

        jvmTest {
            dependencies {
                implementation(libs.org.jetbrains.kotlinx.coroutines.swing)
                implementation(libs.org.junit.jupiter.engine)
            }
        }

        named("androidHostTest") {
            dependencies {
                implementation(libs.junit.junit4)
                implementation(projects.testBalloonIntegrationRobolectric)
                implementation(libs.androidx.test.core)

                // The Compose compiler plugin requires a compose runtime to be present, even if not used here.
                implementation("androidx.compose.ui:ui-test-junit4")
            }
        }

        named("androidDeviceTest") {
            dependencies {
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
