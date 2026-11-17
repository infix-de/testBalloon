import buildLogic.base.compilerPluginId
import buildLogic.base.frameworkCoreArtifactId
import buildLogic.base.gradleJdkVersion
import buildLogic.base.gradlePluginArtifactId
import buildLogic.base.kotlinBaseVersion
import buildLogic.base.kotlinLatestReleaseVersion
import buildLogic.base.rootGroup

plugins {
    id("buildLogic.base.common")
    alias(libs.plugins.org.jetbrains.kotlin.jvm.latest.release)
    alias(libs.plugins.org.jetbrains.kotlin.compiler.plugin.devkit.functional.test.publishing)
    alias(libs.plugins.org.jetbrains.kotlin.compiler.plugin.devkit.gradle.plugin)
}

group = rootGroup

tapmoc {
    kotlin(kotlinLatestReleaseVersion())
    java(gradleJdkVersion())
}

pluginDevKit {
    compilerPlugin("testBalloon-compiler-plugin")

    cliVersions(min = kotlinBaseVersion())
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
