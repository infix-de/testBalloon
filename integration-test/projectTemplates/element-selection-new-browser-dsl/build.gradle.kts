import de.infix.testBalloon.gradlePlugin.layer.kotlin2420RC.withTestBalloon
import org.jetbrains.kotlin.gradle.ExperimentalJsTestDsl
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest

plugins {
    kotlin("multiplatform") version "{{version:org.jetbrains.kotlin.latest.prerelease}}"
    id("com.gradleup.tapmoc") version "{{version:com.gradleup.tapmoc}}"
    id("de.infix.testBalloon") version "{{version:de.infix.testBalloon}}"
}

tapmoc {
    java("{{version:base.jdk}}".toInt())
    kotlin("{{version:org.jetbrains.kotlin.latest.prerelease}}")
}

kotlin {
    js {
        browser {
            @OptIn(ExperimentalJsTestDsl::class)
            test {
                withTestBalloon(project)
                headless = true
                firefox()
            }
        }
    }

    sourceSets {
        commonTest {
            dependencies {
                implementation("de.infix.testBalloon:testBalloon-framework-core:{{version:de.infix.testBalloon}}")
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
