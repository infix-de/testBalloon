import buildLogic.addTestBalloonPluginFromProject
import buildLogic.allTargets
import buildLogic.enableAbiValidation

plugins {
    id("buildLogic.kotlin-multiplatform")
    id("buildLogic.publishing-multiplatform")
}

description = "Library supporting Kotest Assertions with the TestBalloon framework"

addTestBalloonPluginFromProject(projects.testBalloonCompilerPlugin, projects.testBalloonFrameworkShared)

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
