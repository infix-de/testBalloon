@file:OptIn(TestBalloonInternalApi::class)

import buildLogic.addTestBalloonPluginFromProject
import buildLogic.enableAbiValidation
import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalApi

plugins {
    id("buildLogic.kotlin-multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("buildLogic.publishing-multiplatform")
}

description = "Library supporting Robolectric with the TestBalloon framework"

addTestBalloonPluginFromProject(projects.testBalloonCompilerPlugin, projects.testBalloonFrameworkShared)

kotlin {
    enableAbiValidation()

    androidLibrary {
        namespace = "de.infix.testBalloon.integration.robolectric"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withHostTestBuilder {} // Suppresses the warning "Unused Kotlin Source Sets"
    }

    sourceSets {
        androidMain {
            dependencies {
                api(projects.testBalloonFrameworkCore)
                api(libs.org.robolectric)
                implementation(libs.org.jetbrains.kotlin.reflect)
            }
        }

        named("androidHostTest") {
            dependencies {
                implementation(libs.org.jetbrains.kotlin.test) // for assertions and specific JUnit 4 tests
                implementation(libs.junit.junit4)
                implementation(libs.androidx.test.core)
            }
        }
    }
}
