import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest

plugins {
    kotlin("multiplatform") version "{{version:org.jetbrains.kotlin}}"
    id("com.gradleup.tapmoc") version "{{version:com.gradleup.tapmoc}}"
    id("de.infix.testBalloon") version "{{prop:version}}"
}

tapmoc {
    java("{{version:jdk}}".toInt())
    kotlin("{{version:org.jetbrains.kotlin}}")
}

kotlin {
    js {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    iosSimulatorArm64()

    sourceSets {
        commonTest {
            dependencies {
                implementation("de.infix.testBalloon:testBalloon-framework-core:{{prop:version}}")
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
    withType<Test>().configureEach {
        if (includeTestsMatchingPattern != null) filter.includeTestsMatching(includeTestsMatchingPattern)
        testLogging { showStandardStreams = true }
    }
    withType<KotlinJsTest>().configureEach {
        if (includeTestsMatchingPattern != null) filter.includeTestsMatching(includeTestsMatchingPattern)
        testLogging { showStandardStreams = true }
    }
    withType<KotlinNativeTest>().configureEach {
        if (includeTestsMatchingPattern != null) filter.includeTestsMatching(includeTestsMatchingPattern)
        testLogging { showStandardStreams = true }
    }
}
