@file:Suppress("UnstableApiUsage")

plugins {
    id("buildLogic.kotlin-jvm-base")
}

description = "TestBalloon compiler plugin compatibility layer (base) test support"

dependencies {
    api(projects.testBalloonCompilerPlugin)
    compileOnly("$group.compilerPlugin:base")
    // https://github.com/ZacSweers/kotlin-compile-testing/releases
    compileOnly(libs.dev.zacsweers.kctfork)
    compileOnly(libs.org.jetbrains.kotlin.test)

    implementation("$group:testBalloon-framework-shared")

    compileOnly(libs.org.jetbrains.kotlin.stdlib)
    compileOnly(libs.org.jetbrains.kotlin.compiler)
}
