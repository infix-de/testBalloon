import compilerPluginLayer.buildLogic.configurePluginLayer

plugins {
    id("compilerPluginLayer.buildLogic.common")
    // noinspection NewerVersionAvailable
    id("org.jetbrains.kotlin.jvm") version "2.3.20"
}

configurePluginLayer(kotlinVersion = "2.3.20", baseLayer = "kotlin-2-3-0", kctforkVersion = "0.12.1")
