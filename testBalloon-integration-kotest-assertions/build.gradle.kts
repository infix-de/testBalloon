import buildLogic.base.kotestJdkVersion
import buildLogic.kotlin.allTargets
import buildLogic.kotlin.enableAbiValidation

plugins {
    id("buildLogic.base.dokka")
    id("buildLogic.base.publishing")
    id("buildLogic.kotlin.multiplatform")
    id("de.infix.testBalloon")
}

description = "Library supporting Kotest Assertions with the TestBalloon framework"

tapmoc {
    java(kotestJdkVersion())
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
