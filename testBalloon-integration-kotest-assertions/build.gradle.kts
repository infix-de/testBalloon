import buildLogic.allTargets
import buildLogic.enableAbiValidation

plugins {
    id("buildLogic.kotlin-multiplatform")
    id("de.infix.testBalloon")
    id("buildLogic.dokka")
    id("buildLogic.publishing")
}

description = "Library supporting Kotest Assertions with the TestBalloon framework"

kotlin {
    enableAbiValidation()

    allTargets(includeWasmWasi = false)

    sourceSets {
        commonMain {
            dependencies {
                api(projects.testBalloonFrameworkCore)
                api(libs.io.kotest.assertions.core)
            }
        }
    }
}
