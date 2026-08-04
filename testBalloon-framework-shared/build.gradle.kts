import buildLogic.allTargets
import buildLogic.enableAbiValidation

plugins {
    id("buildLogic.kotlin-multiplatform")
    id("buildLogic.publishing")
}

description = "Shared declarations for the TestBalloon framework"

kotlin {
    enableAbiValidation()

    allTargets()
}
