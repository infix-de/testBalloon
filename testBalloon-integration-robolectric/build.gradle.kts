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
                // 
                // This dependency will be activated once PR #10897 is merged and released.
                // Expected in Robolectric 4.15.x or 4.16.
                // Current latest version: 4.15.1 (does not include runner:common yet)
                //
                // When available, uncomment:
                // api("org.robolectric:runner-common:4.15.x")
            }
        }
    }
}
