plugins {
    kotlin("multiplatform") version "{{version:org.jetbrains.kotlin}}"
    id("com.android.kotlin.multiplatform.library") version "{{version:android.gradle.plugin}}"
    id("com.gradleup.tapmoc") version "{{version:com.gradleup.tapmoc}}"
    id("de.infix.testBalloon") version "{{version:de.infix.testBalloon}}"
}

tapmoc {
    java("{{version:jdk}}".toInt())
    kotlin("{{version:org.jetbrains.kotlin}}")
}

kotlin {
    jvm()

    androidLibrary {
        namespace = "org.example.android.multiplatform.library"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withHostTestBuilder {}
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            managedDevices {
                localDevices {
                    @Suppress("UnstableApiUsage")
                    create("pixel2") {
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
                implementation("de.infix.testBalloon:testBalloon-framework-core:{{version:de.infix.testBalloon}}")
            }
        }

        named("androidHostTest") {
            dependencies {
                implementation("de.infix.testBalloon:testBalloon-framework-core:{{version:de.infix.testBalloon}}")
                implementation("junit:junit:{{version:junit.junit4}}")
            }
        }
        named("androidDeviceTest") {
            dependencies {
                implementation("de.infix.testBalloon:testBalloon-framework-core:{{version:de.infix.testBalloon}}")
                implementation("androidx.test:runner:{{version:androidx.test}}")
            }
        }
    }
}

tasks {
    register("listTests") {
        group = "verification"

        val testTaskNames = project.tasks.mapNotNull { task ->
            task.takeIf {
                it.name.endsWith("Test") &&
                    !it.name.startsWith("connected") &&
                    it.javaClass.name.contains("Test") &&
                    !it.javaClass.name.contains("Report")
            }?.run { "##TEST($name)##" }
        }

        doLast {
            println(testTaskNames.joinToString("\n"))
        }
    }

    withType<Test>().configureEach {
        testLogging { showStandardStreams = true }
    }
}
