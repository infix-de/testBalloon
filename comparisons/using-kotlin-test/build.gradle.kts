import buildLogic.versionFromCatalog
import com.android.build.api.dsl.androidLibrary
import tapmoc.Severity

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("com.gradleup.tapmoc")
}

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
                implementation(kotlin("test"))
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
