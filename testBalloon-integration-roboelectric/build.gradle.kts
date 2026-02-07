@file:OptIn(TestBalloonInternalApi::class)

import buildLogic.addTestBalloonPluginFromProject
import buildLogic.enableAbiValidation
import buildLogic.versionFromCatalog
import de.infix.testBalloon.framework.shared.internal.Constants
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

    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=${Constants.SHARED_INTERNAL_PACKAGE_NAME}.TestBalloonInternalApi",
            "-opt-in=${Constants.CORE_PACKAGE_NAME}.TestBalloonExperimentalApi"
        )
    }

    androidLibrary {
        namespace = "de.infix.testBalloon.integration.roboelectric"
        compileSdk = versionFromCatalog("android-compileSdk").toInt()

        withHostTestBuilder {} // Suppresses the warning "Unused Kotlin Source Sets"
    }

    sourceSets {
        androidMain {
            dependencies {
                api(projects.testBalloonFrameworkCore)
                api(libs.org.robolectric)
                implementation(kotlin("reflect"))
            }
        }
    }
}
