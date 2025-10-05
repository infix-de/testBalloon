import buildLogic.addTestBalloonPluginFromProject
import buildLogic.enableAbiValidation

plugins {
    id("buildLogic.multiplatform-excluding-wasm-wasi")
    id("buildLogic.publishing-multiplatform")
}

description = "Library supporting Kotest Assertions with the TestBalloon framework"

addTestBalloonPluginFromProject(projects.testBalloonCompilerPlugin, projects.testBalloonFrameworkAbstractions)

kotlin {
    enableAbiValidation()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.testBalloonFrameworkCore)
                api(libs.io.kotest.assertions.core)
            }
        }
    }
}
