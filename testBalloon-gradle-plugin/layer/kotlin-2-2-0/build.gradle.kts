import gradlePlugin.layer.buildLogic.configurePluginLayer

plugins {
    id("gradlePlugin.layer.buildLogic.common")
    // noinspection NewerVersionAvailable
    id("org.jetbrains.kotlin.jvm") version "2.4.10"
    id("java-gradle-plugin")
}

configurePluginLayer(kotlinVersion = "2.2.0", baseLayer = "base")
