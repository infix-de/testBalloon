import de.infix.testBalloon.gradlePlugin.internal.layer.kotlin2420.withTestBalloon
import org.jetbrains.kotlin.gradle.ExperimentalJsTestDsl

plugins {
    id("org.jetbrains.kotlin.multiplatform") version "2.5.0-Beta1"
}

buildscript {
    dependencies {
        classpath("de.infix.testBalloon:testBalloon-gradle-plugin:$version")
    }
}

@Suppress("AvoidApplyPluginMethod")
apply(plugin = "de.infix.testBalloon")

kotlin {
    js {
        browser {
            @OptIn(ExperimentalJsTestDsl::class)
            test {
                // TODO: remove when autoconfiguration is possible: https://youtrack.jetbrains.com/issue/KT-89230
                withTestBalloon(this@js)
            }
        }
    }

    sourceSets {
        commonTest {
            dependencies {
                implementation("de.infix.testBalloon:testBalloon-framework-core:$version")
            }
        }
    }
}
