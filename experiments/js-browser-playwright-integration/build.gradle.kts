import de.infix.testBalloon.gradlePlugin.internal.layer.kotlin2420.withTestBalloon
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalJsTestDsl
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBrowserDsl
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget

plugins {
    // https://packages.jetbrains.team/maven/p/kt/dev/org/jetbrains/kotlin/kotlin-gradle-plugin/maven-metadata.xml
    id("org.jetbrains.kotlin.multiplatform") version "2.5.0-Beta2"
    id("de.infix.testBalloon") version "1.2.1-SNAPSHOT"
    id("com.gradleup.tapmoc") version "0.5.0"
}

@OptIn(ExperimentalBuildToolsApi::class, ExperimentalKotlinGradlePluginApi::class)
val kotlinVersion: String = kotlin.compilerVersion.get()

tapmoc {
    kotlin(kotlinVersion)
}

val usePlaywright = true

kotlin {
    js {
        browser {
            if (usePlaywright) {
                @OptIn(ExperimentalJsTestDsl::class)
                test {
                    firefox()
                }
            }
        }
        nodejs()
    }

    wasmJs {
        browser {
            if (usePlaywright) {
                @OptIn(ExperimentalJsTestDsl::class)
                test {
                    firefox()
                }
            }
        }
        nodejs()
    }

    sourceSets {
        commonTest {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
                implementation("de.infix.testBalloon:testBalloon-framework-core:$version")
            }
        }
    }
}

// Inside the de.infix.testBalloon Gradle plugin:
kotlin.targets.configureEach {
    val target = this
    if (this is KotlinJsIrTarget) {
        subTargets.configureEach {
            if (this is KotlinJsBrowserDsl) {
                if (usePlaywright) {
                    @OptIn(ExperimentalJsTestDsl::class)
                    test.withTestBalloon(target)
                }
            }
        }
    }
}
