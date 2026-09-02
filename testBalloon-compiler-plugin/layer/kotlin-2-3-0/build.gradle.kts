import compilerPlugin.layer.buildLogic.configurePluginLayer

plugins {
    id("compilerPlugin.layer.buildLogic.common")
    // noinspection NewerVersionAvailable
    id("org.jetbrains.kotlin.jvm") version "2.3.0"
}

configurePluginLayer(kotlinVersion = "2.3.0", baseLayer = "kotlin-2-2-0", kctforkVersion = "0.12.1")
