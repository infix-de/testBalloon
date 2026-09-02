import gradlePlugin.layer.buildLogic.configurePluginLayer

plugins {
    id("gradlePlugin.layer.buildLogic.common")
    // noinspection NewerVersionAvailable
    id("org.jetbrains.kotlin.jvm") version "2.4.20-RC"
    id("java-gradle-plugin")
}

configurePluginLayer(kotlinVersion = "2.4.20-RC", baseLayer = "kotlin-2-3-20")
