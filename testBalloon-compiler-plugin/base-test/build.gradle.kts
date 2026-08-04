@file:Suppress("UnstableApiUsage")

plugins {
    id("buildLogic.kotlin-jvm-base")
}

description = "TestBalloon compiler plugin compatibility layer (base) test support"

dependencies {
    compileOnly(projects.testBalloonCompilerPlugin.base)
    api(projects.testBalloonCompilerPlugin)
    api(libs.dev.zacsweers.kctfork)
    compileOnly(libs.org.jetbrains.kotlin.test)

    implementation(projects.testBalloonFrameworkShared)

    compileOnly(libs.org.jetbrains.kotlin.stdlib)
    compileOnly(libs.org.jetbrains.kotlin.compiler)
}
