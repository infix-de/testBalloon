import buildLogic.addTestBalloonPluginFromProject

plugins {
    id("buildLogic.multiplatform")
}

addTestBalloonPluginFromProject(projects.testBalloonCompilerPlugin, projects.testBalloonFrameworkAbstractions)

kotlin {
    compilerOptions {
        // freeCompilerArgs.addAll("-P", "plugin:de.infix.testBalloon:debugLevel=DISCOVERY")
    }

    sourceSets {
        commonTest {
            dependencies {
                // implementation(libs.de.infix.testBalloon.framework.core) // Use this outside this project
                implementation(projects.testBalloonFrameworkCore)
                implementation(kotlin("test")) // for assertions only
            }
        }
    }
}
