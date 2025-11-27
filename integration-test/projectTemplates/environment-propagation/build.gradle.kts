import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest

plugins {
    kotlin("multiplatform") version "{{version:org.jetbrains.kotlin}}"
    id("com.android.kotlin.multiplatform.library") version "{{version:android.gradle.plugin}}"
    id("com.gradleup.tapmoc") version "{{version:com.gradleup.tapmoc}}"
    id("de.infix.testBalloon") version "{{prop:version}}"
}

tapmoc {
    java("{{version:jdk}}".toInt())
    kotlin("{{version:org.jetbrains.kotlin}}")
}

testBalloon {
    browserSafeEnvironmentPattern = "FROM_EXTENSION"
}

kotlin {
    jvm()

    js {
        nodejs()
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        nodejs()
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi {
        nodejs()
    }

    // Kotlin/Native target support – see https://kotlinlang.org/docs/native-target-support.html
    // Tier 1
    macosX64()
    // Tier 2
    linuxX64()
    // Tier 3
    mingwX64()

    fun androidSdkAvailable() = providers.environmentVariable("ANDROID_HOME").isPresent

    androidLibrary {
        namespace = "org.example.android.multiplatform.library"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        if (androidSdkAvailable()) {
            withHostTestBuilder {}
        }
    }

    sourceSets {
        commonTest {
            dependencies {
                implementation("de.infix.testBalloon:testBalloon-framework-core:{{prop:version}}")
            }
        }

        if (androidSdkAvailable()) {
            named("androidHostTest") {
                dependencies {
                    implementation("de.infix.testBalloon:testBalloon-framework-core:{{prop:version}}")
                    implementation("junit:junit:{{version:junit.junit4}}")
                }
            }
        }
    }
}

tasks {
    register("listTests") {
        group = "verification"

        val testTaskNames = project.tasks.mapNotNull { task ->
            task.takeIf {
                it.name.endsWith("Test") && it.javaClass.name.contains("Test") &&
                    !it.javaClass.name.contains("Report")
            }?.run { "##TEST($name)##" }
        }

        doLast {
            println(testTaskNames.joinToString("\n"))
        }
    }

    val includeTestsMatchingPattern = project.findProperty("local.includeTestsMatching") as? String
    withType<Test> {
        if (includeTestsMatchingPattern != null) filter.includeTestsMatching(includeTestsMatchingPattern)
        testLogging { showStandardStreams = true }
    }
    withType<KotlinJsTest> {
        if (includeTestsMatchingPattern != null) filter.includeTestsMatching(includeTestsMatchingPattern)
        testLogging { showStandardStreams = true }
    }
    withType<KotlinNativeTest> {
        if (includeTestsMatchingPattern != null) filter.includeTestsMatching(includeTestsMatchingPattern)
        testLogging { showStandardStreams = true }
    }
}
