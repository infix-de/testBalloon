plugins {
    id("buildLogic.base.common")
    kotlin("jvm") version "2.4.21"
    pluginDevKit("gradle-plugin")
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

val Project.rootGroup: String get() = "${project.property("local.PROJECT_ROOT_GROUP")}"
val Project.compilerPluginId: String get() = "${project.property("local.PROJECT_COMPILER_PLUGIN_ID")}"
val Project.frameworkCoreArtifactId: String get() = "${project.property("local.PROJECT_FRAMEWORK_CORE_ARTIFACT_ID")}"
val Project.gradlePluginArtifactId: String get() = "${project.property("local.PROJECT_GRADLE_PLUGIN_ARTIFACT_ID")}"

tasks.withType<Test>().configureEach {
    // https://docs.gradle.org/current/userguide/java_testing.html
    useJUnitPlatform()
}
