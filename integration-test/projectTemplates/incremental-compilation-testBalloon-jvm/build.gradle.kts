import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest

plugins {
    kotlin("jvm") version "{{version:org.jetbrains.kotlin}}"
    id("com.gradleup.tapmoc") version "{{version:com.gradleup.tapmoc}}"
    id("de.infix.testBalloon") version "{{version:de.infix.testBalloon}}"
}

tapmoc {
    java("{{version:jdk}}".toInt())
    kotlin("{{version:org.jetbrains.kotlin}}")
}

dependencies {
    implementation("de.infix.testBalloon:testBalloon-framework-core:{{version:de.infix.testBalloon}}")
}

tasks {
    register("listTests") {
        group = "verification"

        doLast {
            println("##TEST(test)##")
        }
    }

    withType<Test>().configureEach {
        testLogging { showStandardStreams = true }
    }
}
