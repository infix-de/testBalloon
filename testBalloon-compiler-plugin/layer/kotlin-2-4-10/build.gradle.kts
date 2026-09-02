import compilerPlugin.layer.buildLogic.configurePluginLayer

plugins {
    id("compilerPlugin.layer.buildLogic.common")
    // noinspection NewerVersionAvailable
    id("org.jetbrains.kotlin.jvm") version "2.4.10"
}

configurePluginLayer(kotlinVersion = "2.4.10", kctforkVersion = "0.13.0")
