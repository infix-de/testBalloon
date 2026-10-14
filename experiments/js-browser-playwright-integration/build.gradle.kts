import de.infix.testBalloon.gradlePlugin.internal.layer.kotlin2420.withTestBalloon
import org.jetbrains.kotlin.gradle.ExperimentalJsTestDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import kotlin.time.Duration.Companion.seconds

plugins {
    // https://packages.jetbrains.team/maven/p/kt/dev/org/jetbrains/kotlin/kotlin-gradle-plugin/maven-metadata.xml
    id("org.jetbrains.kotlin.multiplatform") version "2.5.0-Beta2-71"
    id("de.infix.testBalloon") version "1.2.0-RC2-SNAPSHOT"
    id("com.gradleup.tapmoc") version "0.5.0"
}

tapmoc {
    kotlin("2.5.0-Beta2-71")
}

// TODO: Enable Playwright integration on JS/Wasm once KT-89269 is resolved.
val disablePlaywrightOnJsWasmForKt89269 = true

kotlin {
    js {
        browser {
            @OptIn(ExperimentalJsTestDsl::class)
            test {
                firefox()
            }
        }
    }

    wasmJs {
        browser {
            if (disablePlaywrightOnJsWasmForKt89269) return@browser

            @OptIn(ExperimentalJsTestDsl::class)
            test {
                timeout = 3.seconds
                firefox()
            }
        }
    }

    sourceSets {
        commonTest {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib:2.5.0-Beta2-71")
                implementation("de.infix.testBalloon:testBalloon-framework-core:$version")
            }
        }
    }
}

// Inside the de.infix.testBalloon Gradle plugin:
kotlin.targets.configureEach {
    if (name == "wasmJs" && disablePlaywrightOnJsWasmForKt89269) return@configureEach

    val target = this
    if (this is KotlinJsTargetDsl) {
        // TODO: The following block should be something like: browsers.configureEach { ... }
        browser {
            @OptIn(ExperimentalJsTestDsl::class)
            test {
                withTestBalloon(target)
            }
        }
    }
}
