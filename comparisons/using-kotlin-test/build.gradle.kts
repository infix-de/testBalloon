import buildLogic.jdkVersion
import buildLogic.kotlinVersion
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import org.jetbrains.kotlin.gradle.ExperimentalJsTestDsl
import tapmoc.Severity
import java.time.Duration

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("com.gradleup.tapmoc")
}

tapmoc {
    java(jdkVersion())
    kotlin(kotlinVersion())

    checkDependencies(Severity.ERROR)
}

kotlin {
    jvm()

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
                chromium {
                    // Override the common timeout option
                    timeout = Duration.ofSeconds(5)
                    launchArgs.add("--no-sandbox")
                }
            }
        }
    }

    extensions.configure<KotlinMultiplatformAndroidLibraryExtension>("androidLibrary") {
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
        commonTest {
            dependencies {
                implementation(libs.org.jetbrains.kotlin.test)
            }
        }

        named("androidDeviceTest") {
            dependencies {
                implementation(libs.androidx.test.runner)
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    // https://docs.gradle.org/current/userguide/java_testing.html
    useJUnitPlatform()
}
