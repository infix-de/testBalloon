import compilerPlugin.layer.buildLogic.configurePluginLayer
import compilerPlugin.layer.buildLogic.rootGroup

plugins {
    id("compilerPlugin.layer.buildLogic.common")
    // noinspection NewerVersionAvailable
    id("org.jetbrains.kotlin.jvm") version "2.2.0"
    alias(libs.plugins.com.github.gmazzo.buildconfig)
}

configurePluginLayer(kotlinVersion = "2.2.0")

dependencies {
    api("$rootGroup:testBalloon-framework-shared")
}

buildConfig {
    packageName("$group.buildConfig")
    useKotlinOutput { internalVisibility = true }

    buildConfigField("String", "PROJECT_VERSION", "\"$version\"")
    buildConfigField("String", "PROJECT_ROOT_GROUP", "\"$rootGroup\"")
    buildConfigField("String", "PROJECT_FRAMEWORK_CORE_ARTIFACT_ID", "\"testBalloon-framework-core\"")

    buildConfigField(
        "String",
        "PROJECT_COMPILER_PLUGIN_ID",
        "\"${project.property("local.PROJECT_COMPILER_PLUGIN_ID")}\""
    )
}
