import buildLogic.allTargets
import buildLogic.dokkaEnableNavigationNodeHiding
import buildLogic.enableAbiValidation
import buildLogic.propagateLifecycleTasksToIncludedBuilds
import buildLogic.rootGroup
import buildLogic.versionFromCatalog
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest

plugins {
    id("buildLogic.kotlin-multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.kotlin.plugin.atomicfu")
    id("buildLogic.dokka")
    id("buildLogic.publishing")
}

description = "Core library for the TestBalloon framework"

kotlin {
    enableAbiValidation()

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=de.infix.testBalloon.framework.shared.internal.TestBalloonInternalApi",
            "-opt-in=de.infix.testBalloon.framework.shared.internal.TestBalloonInternalTestingApi",
            "-opt-in=de.infix.testBalloon.framework.core.TestBalloonExperimentalApi"
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
        // compilerOptions {
        //     freeCompilerArgs.add("-opt-in=kotlin.js.ExperimentalWasmJsInterop")
        // }
    }

    android {
        namespace = "de.infix.testBalloon.framework.core"
        compileSdk = versionFromCatalog("android-compileSdk").toInt()

        withHostTestBuilder {}.configure {}
    }

    sourceSets {
        commonMain {
            dependencies {
                api("$rootGroup:testBalloon-framework-shared:$version")
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
                implementation(libs.org.jetbrains.kotlin.test)
            }
        }

        jvmTest {
            dependencies {
                implementation(libs.org.jetbrains.kotlinx.coroutines.swing)
            }
        }
    }
}

dokkaEnableNavigationNodeHiding()

tasks.withType<Test>().configureEach {
    // https://docs.gradle.org/current/userguide/java_testing.html
    useJUnitPlatform {
        excludeEngines("de.infix.testBalloon") // Do not use TestBalloon in this project
    }
}

tasks.withType<KotlinNativeTest>().configureEach {
    val taskName = name
    doFirst {
        environment("TEST_TASK_NAME", taskName, false)
        environment("SIMCTL_CHILD_TEST_TASK_NAME", taskName, false) // Apple simulator execution environment
        System.getenv("CI")?.let {
            environment("SIMCTL_CHILD_CI", it, false) // Apple simulator execution environment
        }
    }
}

afterEvaluate {
    val publishableIncludeBuilds =
        listOf("testBalloon-compiler-plugin", "testBalloon-framework-shared", "testBalloon-gradle-plugin")

    for (taskName in listOf(
        "publishAllPublicationsToAggregationStagingRepository",
        "publishAllPublicationsToIntegrationTestRepository",
        "publishAllPublicationsToLocalRepository",
        "publishToMavenLocal"
    )) {
        tasks.named(taskName) {
            dependsOn(publishableIncludeBuilds.map { gradle.includedBuild(it).task(":$taskName") })
        }
    }
}

propagateLifecycleTasksToIncludedBuilds()
