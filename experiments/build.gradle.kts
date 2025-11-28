import buildLogic.addTestBalloonPluginFromProject
import buildLogic.jsTargets
import buildLogic.nativeTargets
import buildLogic.versionFromCatalog
import tapmoc.Severity

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.gradleup.tapmoc")
}

addTestBalloonPluginFromProject(projects.testBalloonCompilerPlugin, projects.testBalloonFrameworkShared)

tapmoc {
    java(versionFromCatalog("jdk").toInt())
    kotlin(versionFromCatalog("org.jetbrains.kotlin"))

    checkApiDependencies(Severity.ERROR)
    checkRuntimeDependencies(Severity.ERROR)
}

kotlin {
    compilerOptions {
        // freeCompilerArgs.addAll("-P", "plugin:de.infix.testBalloon:debugLevel=DISCOVERY")
    }

    jvm()
    jsTargets()
    nativeTargets()

    sourceSets {
        commonTest {
            dependencies {
                implementation(projects.testBalloonFrameworkCore)
                implementation(kotlin("test")) // for assertions only
            }
        }
    }
}
