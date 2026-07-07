import buildLogic.allTargets
import buildLogic.enableAbiValidation

plugins {
    id("buildLogic.kotlin-multiplatform")
    id("buildLogic.publishing-multiplatform")
}

description = "Shared declarations for the TestBalloon framework"

kotlin {
    enableAbiValidation()

    allTargets()

    sourceSets {
        commonMain {
            dependencies {
                // This module is intended to be included into the compiler plugin's shadow Jar without pulling
                // in the kotlin stdlib (relevant on the JVM only).
                compileOnly(libs.org.jetbrains.kotlin.stdlib)
            }
        }

        named("nonJvmMain") {
            dependencies {
                // The following avoids "Unsupported `compileOnly` Dependencies in Kotlin Targets" for non-JVM targets.
                api(libs.org.jetbrains.kotlin.stdlib)
            }
        }
    }
}
