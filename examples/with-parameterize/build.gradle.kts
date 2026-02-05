import buildLogic.addTestBalloonPluginFromProject
import buildLogic.allTargets

plugins {
    id("buildLogic.kotlin-multiplatform")
}

// The following invocation supplements the TestBalloon plugin declaration inside this project:
addTestBalloonPluginFromProject(projects.testBalloonCompilerPlugin, projects.testBalloonFrameworkShared)

kotlin {
    allTargets()

    sourceSets {
        commonTest {
            dependencies {
                // required for TestBalloon outside this project:
                //     implementation("de.infix.testBalloon:testBalloon-framework-core:${testBalloonVersion}")
                // instead of this project-internal dependency:
                implementation(projects.testBalloonFrameworkCore)

                implementation(kotlin("test")) // for assertions only
                implementation(libs.com.benwoodworth.parameterize)
            }
        }
    }
}
