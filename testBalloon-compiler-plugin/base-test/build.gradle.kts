@file:Suppress("UnstableApiUsage")

import buildLogic.rootGroup

plugins {
    id("buildLogic.kotlin-jvm-base")
}

description = "TestBalloon compiler plugin compatibility layer (base) test support"

dependencies {
    api(projects.testBalloonCompilerPlugin)
    compileOnly("$rootGroup.compilerPlugin.layer:base")
    // https://github.com/ZacSweers/kotlin-compile-testing/releases
    compileOnly(libs.dev.zacsweers.kctfork)
    compileOnly(libs.org.jetbrains.kotlin.test)

    implementation("$rootGroup:testBalloon-framework-shared")

    compileOnly(libs.org.jetbrains.kotlin.stdlib)
    compileOnly(libs.org.jetbrains.kotlin.compiler)
}
