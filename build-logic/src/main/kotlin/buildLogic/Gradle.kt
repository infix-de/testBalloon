package buildLogic

import org.gradle.api.invocation.Gradle

fun Gradle.rootBuild(): Gradle = parent.let { it?.rootBuild() ?: this }
