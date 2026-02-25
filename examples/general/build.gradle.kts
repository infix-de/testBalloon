import buildLogic.addTestBalloonPluginFromProject
import buildLogic.allTargets

plugins {
    id("buildLogic.kotlin-multiplatform")
    // id("de.infix.testBalloon") version "$testBalloonVersion"  // required for TestBalloon outside this project
    id("org.jetbrains.kotlin.plugin.atomicfu")
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

                implementation(libs.org.jetbrains.kotlin.test) // for assertions only
                implementation(libs.org.jetbrains.kotlinx.atomicfu)
            }
        }

        jvmTest {
            dependencies {
                implementation(libs.org.jetbrains.kotlinx.coroutines.debug)
            }
        }
    }
}
