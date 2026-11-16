import gradlePlugin.layer.buildLogic.configurePluginLayer

plugins {
    id("gradlePlugin.layer.buildLogic")
}

configurePluginLayer(kotlinVersion = "2.4.20", baseLayer = "kotlin-2-3-20")
