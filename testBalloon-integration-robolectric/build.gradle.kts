import buildLogic.addTestBalloonPluginFromProject
import buildLogic.enableAbiValidation

plugins {
    id("buildLogic.multiplatform-plus-android-library")
    id("buildLogic.publishing-multiplatform")
}

description = "Library supporting Robolectric integration with the TestBalloon framework"

addTestBalloonPluginFromProject(projects.testBalloonCompilerPlugin, projects.testBalloonFrameworkShared)

kotlin {
    enableAbiValidation()

    androidLibrary {
        namespace = "de.infix.testBalloon.integration.robolectric"
    }

    sourceSets {
        commonMain {
            dependencies {
                api(projects.testBalloonFrameworkCore)
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test")) // for assertions only
            }
        }

        jvmMain {
            dependencies {
                // Robolectric runner:common module provides framework-agnostic integration API
                // Based on https://github.com/robolectric/robolectric/pull/10897
                // This is a compileOnly dependency as 4.15-SNAPSHOT is not yet available
                // compileOnly("org.robolectric:runner-common:4.15-SNAPSHOT")
            }
        }
    }
}
