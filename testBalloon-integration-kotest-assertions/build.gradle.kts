import buildLogic.kotlin.allTargets
import buildLogic.kotlin.enableAbiValidation

plugins {
    id("buildLogic.kotlin.multiplatform")
    id("de.infix.testBalloon")
    id("buildLogic.base.dokka")
    id("buildLogic.base.publishing")
}

description = "Library supporting Kotest Assertions with the TestBalloon framework"

tapmoc {
    java(11) // required for Kotest >= 6.0
}

kotlin {
    enableAbiValidation()

    allTargets()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.testBalloonFrameworkCore)
                api(libs.io.kotest.assertions.core)
            }
        }
    }
}
