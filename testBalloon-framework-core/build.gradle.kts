import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id("buildLogic.multiplatform-plus-android-library")
    id("buildLogic.publishing-multiplatform")
}

description = "Core library for the TestBalloon framework"

kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation {
        enabled = true
    }
    explicitApi()

    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=de.infix.testBalloon.framework.internal.TestBalloonInternalApi",
            "-opt-in=de.infix.testBalloon.framework.TestBalloonExperimentalApi"
        )
    }

    js {
        // The core library tests use kotlin-test, which comes with a default timeout of 2 seconds on JS.
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
        namespace = "de.infix.testBalloon.framework.core"
    }

    sourceSets {
        commonMain {
            dependencies {
                api(projects.testBalloonFrameworkAbstractions)
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
        excludeEngines("de.infix.testBalloon")
    }
}
