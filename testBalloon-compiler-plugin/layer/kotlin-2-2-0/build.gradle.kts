import compilerPlugin.layer.buildLogic.configurePluginLayer

plugins {
    id("compilerPlugin.layer.buildLogic.common")
    // noinspection NewerVersionAvailable
    id("org.jetbrains.kotlin.jvm") version "2.2.0"
}

configurePluginLayer(kotlinVersion = "2.2.0", baseLayer = "base", kctforkVersion = "0.10.1")
