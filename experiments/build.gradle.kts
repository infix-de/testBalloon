import buildLogic.addTestBalloonPluginFromProject
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

    checkDependencies(Severity.ERROR)
}

kotlin {
    compilerOptions {
        // freeCompilerArgs.addAll("-P", "plugin:de.infix.testBalloon:debugLevel=DISCOVERY")
    }

    jvm()

    sourceSets {
        commonTest {
            dependencies {
                implementation(projects.testBalloonFrameworkCore)
                implementation(kotlin("test")) // for assertions only
            }
        }
    }
}
