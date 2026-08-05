@file:Suppress("UnstableApiUsage")

plugins {
    id("buildLogic.kotlin-jvm-base")
}

description = "TestBalloon compiler plugin compatibility layer (base) test support"

dependencies {
    api(projects.testBalloonCompilerPlugin)
    compileOnly(projects.testBalloonCompilerPlugin.compilerPluginLayerBase)
    // https://github.com/ZacSweers/kotlin-compile-testing/releases
    compileOnly(libs.dev.zacsweers.kctfork)
    compileOnly(libs.org.jetbrains.kotlin.test)

    implementation(projects.testBalloonFrameworkShared)

    compileOnly(libs.org.jetbrains.kotlin.stdlib)
    compileOnly(libs.org.jetbrains.kotlin.compiler)
}
