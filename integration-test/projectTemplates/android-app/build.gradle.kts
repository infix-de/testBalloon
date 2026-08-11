import org.gradle.util.internal.VersionNumber

plugins {
    id("com.android.application") version "{{version:android.gradle.plugin}}"
    "{{version:org.jetbrains.kotlin}}".takeIf { it.isNotEmpty() }?.let {
        if ("{{version:android.gradle.plugin}}".takeWhile { it.isDigit() }.toInt() < 9) {
            id("org.jetbrains.kotlin.android") version it
        }
    }
    id("com.gradleup.tapmoc") version "{{version:com.gradleup.tapmoc}}"
    id("de.infix.testBalloon") version "{{version:de.infix.testBalloon}}"
}

tapmoc {
    java("{{version:jdk}}".toInt())
    "{{version:org.jetbrains.kotlin}}".takeIf { it.isNotEmpty() }?.let {
        kotlin(it)
    }
}

android {
    namespace = "org.example.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.example.android"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        @Suppress("UnstableApiUsage")
        testOptions {
            animationsDisabled = true
            managedDevices {
                localDevices {
                    create("pixel2") {
                        device = "Pixel 2"
                        apiLevel = 30
                        systemImageSource = "aosp"
                    }
                }
            }
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1,LICENSE*.md}"
        }
    }
}

dependencies {
    testImplementation("de.infix.testBalloon:testBalloon-framework-core:{{version:de.infix.testBalloon}}")
    testImplementation("junit:junit:{{version:junit.junit4}}")
    androidTestImplementation("de.infix.testBalloon:testBalloon-framework-core:{{version:de.infix.testBalloon}}")
    androidTestImplementation("androidx.test:runner:{{version:androidx.test}}")
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
