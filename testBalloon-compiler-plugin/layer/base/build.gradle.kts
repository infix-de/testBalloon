import buildLogic.rootGroup

plugins {
    id("buildLogic.kotlin-jvm-base")
    alias(libs.plugins.com.github.gmazzo.buildconfig)
}

description = "TestBalloon compiler plugin compatibility layer (base)"

group = "$rootGroup.compilerPlugin.layer"

dependencies {
    api("$rootGroup:testBalloon-framework-shared")
    compileOnly(libs.org.jetbrains.kotlin.compiler)
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
