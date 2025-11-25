import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest

plugins {
    kotlin("jvm") version "{{version:org.jetbrains.kotlin}}"
    id("com.gradleup.compat.patrouille") version "{{version:com.gradleup.compat.patrouille}}"
    id("de.infix.testBalloon") version "{{prop:version}}"
}

compatPatrouille {
    java("{{version:jdk}}".toInt())
    kotlin("{{version:org.jetbrains.kotlin}}")
}

dependencies {
    implementation("de.infix.testBalloon:testBalloon-framework-core:{{prop:version}}")
}

tasks {
    register("listTests") {
        group = "verification"

        doLast {
            println("##TEST(test)##")
        }
    }

    withType<Test> {
        testLogging { showStandardStreams = true }
    }
}
