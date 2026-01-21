@file:OptIn(TestBalloonInternalApi::class)

import buildLogic.addTestBalloonPluginFromProject
import buildLogic.enableAbiValidation
import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalApi

plugins {
    id("buildLogic.kotlin-multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("buildLogic.publishing-multiplatform")
}

description = "Library supporting Roboelectric with the TestBalloon framework"

addTestBalloonPluginFromProject(projects.testBalloonCompilerPlugin, projects.testBalloonFrameworkShared)

kotlin {
    enableAbiValidation()

    androidLibrary {
        namespace = "de.infix.testBalloon.integration.roboelectric"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        // With AGP 8.3.2, Android+KMP host tests could not be configured
        // withHostTestBuilder {} // Suppresses the warning "Unused Kotlin Source Sets"
    }

    sourceSets {
        androidMain {
            dependencies {
                api(projects.testBalloonFrameworkCore)
                api(libs.org.robolectric)
                implementation(kotlin("reflect"))
            }
        }

        // With AGP 8.3.2, Android+KMP host tests could not be configured
        // named("androidHostTest") {
        //     dependencies {
        //         implementation(kotlin("test")) // for assertions and specific JUnit 4 tests
        //         implementation(libs.junit.junit4)
        //         implementation(libs.androidx.test.core)
        //     }
        // }
    }
}
