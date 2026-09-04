import gradlePlugin.layer.buildLogic.configurePluginLayer
import gradlePlugin.layer.buildLogic.libraryFromCatalog

plugins {
    id("gradlePlugin.layer.buildLogic.common")
    // noinspection NewerVersionAvailable
    id("org.jetbrains.kotlin.jvm") version "2.4.10"
    id("java-gradle-plugin")
    alias(libs.plugins.com.github.gmazzo.buildconfig)
}

configurePluginLayer(kotlinVersion = "2.2.0")

description = "TestBalloon Gradle plugin compatibility layer (base)"

val rootGroup = "${project.property("local.PROJECT_GROUP_ID")}"

dependencies {
    api("$rootGroup:testBalloon-framework-shared:$version")
}

buildConfig {
    packageName("buildConfig")
    useKotlinOutput { internalVisibility = true }

    buildConfigField(
        "String",
        "PROJECT_COMPILER_PLUGIN_ID",
        "\"${project.property("local.PROJECT_COMPILER_PLUGIN_ID")}\""
    )
    buildConfigField("String", "PROJECT_VERSION", "\"$version\"")
    buildConfigField("String", "PROJECT_GROUP_ID", "\"$rootGroup\"")
    buildConfigField(
        "String",
        "PROJECT_JUNIT_PLATFORM_LAUNCHER",
        "\"${libraryFromCatalog("org.junit.platform.launcher")}\""
    )
}
