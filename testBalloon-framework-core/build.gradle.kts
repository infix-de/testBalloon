@file:OptIn(TestBalloonInternalApi::class)

import buildLogic.allTargets
import buildLogic.enableAbiValidation
import buildLogic.versionFromCatalog
import de.infix.testBalloon.framework.shared.internal.Constants
import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest

plugins {
    id("buildLogic.kotlin-multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("buildLogic.publishing-multiplatform")
    id("org.jetbrains.kotlin.plugin.atomicfu")
}

description = "Core library for the TestBalloon framework"

kotlin {
    enableAbiValidation()

    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=${Constants.SHARED_INTERNAL_PACKAGE_NAME}.TestBalloonInternalApi",
            "-opt-in=${Constants.CORE_PACKAGE_NAME}.TestBalloonExperimentalApi"
        )
    }

    allTargets()

    js {
        // The core library tests use kotlin.test, which comes with a default timeout of 2 seconds on JS.
        // This may be too restrictive on slow CI runners, so we are increasing it.
        val kotlinTestTimeout = "10s"
        nodejs { testTask { useMocha { timeout = kotlinTestTimeout } } }
        browser { testTask { useMocha { timeout = kotlinTestTimeout } } }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        compilerOptions {
            freeCompilerArgs.add("-opt-in=kotlin.js.ExperimentalWasmJsInterop")
        }
    }

    androidLibrary {
        namespace = Constants.CORE_PACKAGE_NAME
        compileSdk = versionFromCatalog("android-compileSdk").toInt()
    }

    sourceSets {
        commonMain {
            dependencies {
                api(projects.testBalloonFrameworkShared)
                api(libs.org.jetbrains.kotlinx.coroutines.core)
                api(libs.org.jetbrains.kotlinx.coroutines.test)
                implementation(libs.org.jetbrains.kotlinx.kotlinx.datetime)
                implementation(libs.org.jetbrains.kotlinx.atomicfu)
            }
        }

        jvmMain {
            dependencies {
                implementation(libs.org.junit.platform.engine)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.androidx.test.core)
                implementation(libs.junit.junit4)
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        jvmTest {
            dependencies {
                implementation(libs.org.jetbrains.kotlinx.coroutines.swing)
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    // https://docs.gradle.org/current/userguide/java_testing.html
    useJUnitPlatform {
        excludeEngines(Constants.JUNIT_ENGINE_ID) // Do not use TestBalloon in this project
    }
}

tasks.withType<KotlinNativeTest>().configureEach {
    val taskName = name
    doFirst {
        environment("TEST_TASK_NAME", taskName, false)
        environment("SIMCTL_CHILD_TEST_TASK_NAME", taskName, false) // Apple simulator execution environment
    }
}
