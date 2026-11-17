import buildLogic.kotlin.allTargets
import buildLogic.kotlin.applyWebpackVersionsFix
import buildLogic.kotlin.enableAbiValidation

plugins {
    id("buildLogic.base.dokka")
    id("buildLogic.base.publishing")
    id("buildLogic.kotlin.multiplatform")
    alias(libs.plugins.org.jetbrains.kotlin.compiler.plugin.devkit.functional.test.publishing)
}

description = "Shared declarations for the TestBalloon framework"

kotlin {
    enableAbiValidation()

    allTargets()

    sourceSets {
        jvmTest {
            dependencies {
                implementation(libs.org.jetbrains.kotlin.test)
                implementation(libs.io.kotest.assertions.core)
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

applyWebpackVersionsFix()
