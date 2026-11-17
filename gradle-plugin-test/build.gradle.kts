import buildLogic.base.compilerPluginId
import buildLogic.base.frameworkCoreArtifactId
import buildLogic.base.gradlePluginArtifactId
import buildLogic.base.rootGroup

plugins {
    id("buildLogic.base.common")
    kotlin("jvm") version "2.4.21"
    alias(libs.plugins.org.jetbrains.kotlin.compiler.plugin.devkit.functional.test.publishing)
    alias(libs.plugins.org.jetbrains.kotlin.compiler.plugin.devkit.gradle.plugin)
}

group = rootGroup

tapmoc {
    kotlin("2.4.21")
    java(24)
}

pluginDevKit {
    compilerPlugin("testBalloon-compiler-plugin")

    cliVersions(min = "2.2.21")
}

buildConfig {
    buildConfigField("String", "PROJECT_VERSION", "\"$version\"")
    buildConfigField("String", "PROJECT_ROOT_GROUP", "\"$rootGroup\"")
    buildConfigField("String", "PROJECT_FRAMEWORK_CORE_ARTIFACT_ID", "\"$frameworkCoreArtifactId\"")
    buildConfigField("String", "PROJECT_COMPILER_PLUGIN_ID", "\"$compilerPluginId\"")
}

// WORKAROUND: Make 'main' source set visible in tests
kotlin { target { compilations { getByName("functionalTest").associateWith(getByName("main")) } } }

dependencies {
    functionalTestImplementation("$rootGroup:$frameworkCoreArtifactId:$version")
    functionalTestRuntimeOnly("$rootGroup:$gradlePluginArtifactId:$version")
}

tasks.withType<Test>().configureEach {
    // https://docs.gradle.org/current/userguide/java_testing.html
    useJUnitPlatform()
}
