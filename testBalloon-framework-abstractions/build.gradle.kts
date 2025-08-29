plugins {
    id("buildLogic.multiplatform")
    id("buildLogic.publishing-multiplatform")
}

description = "Shared abstractions for the TestBalloon framework"

kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation {
        enabled = true
    }
    explicitApi()
}
