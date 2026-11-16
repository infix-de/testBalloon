import gradlePlugin.layer.buildLogic.configurePluginLayer

plugins {
    id("gradlePlugin.layer.buildLogic")
}

configurePluginLayer(kotlinVersion = "2.3.20", baseLayer = "kotlin-2-2-0")
