import buildLogic.enableAbiValidation

plugins {
    id("buildLogic.multiplatform")
    id("buildLogic.publishing-multiplatform")
}

description = "Shared abstractions for the TestBalloon framework"

kotlin {
    enableAbiValidation()
}
