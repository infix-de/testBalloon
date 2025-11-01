import buildLogic.enableAbiValidation

plugins {
    id("buildLogic.multiplatform")
    id("buildLogic.publishing-multiplatform")
    id("buildLogic.dokka")
}

description = "Shared declarations for the TestBalloon framework"

kotlin {
    enableAbiValidation()
}

dependencies {
    dokkaPlugin(project(":documentation:dokka-plugin-hide-internal-api"))
}
