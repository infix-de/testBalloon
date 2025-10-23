import buildLogic.enableAbiValidation

plugins {
    id("buildLogic.multiplatform")
    id("buildLogic.publishing-multiplatform")
}

description = "Shared declarations for the TestBalloon framework"

kotlin {
    enableAbiValidation()
}
