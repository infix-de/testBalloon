import gradlePluginLayer.buildLogic.configurePluginLayer

plugins {
    id("gradlePluginLayer.buildLogic.common")
    // noinspection NewerVersionAvailable
    id("org.jetbrains.kotlin.jvm") version "2.2.0"
    id("java-gradle-plugin")
    id("buildLogic.publishing")
}

configurePluginLayer(kotlinVersion = "2.2.0", baseLayer = "base")
