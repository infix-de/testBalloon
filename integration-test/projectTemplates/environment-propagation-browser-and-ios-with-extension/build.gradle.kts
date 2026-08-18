import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.BaseNpmExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.LockFileMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmExtension
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.npm.WasmNpmExtension
import kotlin.apply

plugins {
    kotlin("multiplatform") version "{{version:org.jetbrains.kotlin}}"
    id("com.gradleup.tapmoc") version "{{version:com.gradleup.tapmoc}}"
    id("de.infix.testBalloon") version "{{version:de.infix.testBalloon}}"
}

tapmoc {
    java("{{version:jdk}}".toInt())
    kotlin("{{version:org.jetbrains.kotlin}}")
}

testBalloon {
    browserSafeEnvironmentPattern = "CUSTOM_TWO"
    simulatorSafeEnvironmentPattern = "CUSTOM_TWO"
}

kotlin {
    js {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    iosSimulatorArm64()

    sourceSets {
        commonTest {
            dependencies {
                implementation("de.infix.testBalloon:testBalloon-framework-core:{{version:de.infix.testBalloon}}")
            }
        }
    }
}

tasks {
    register("listTests") {
        group = "verification"

        val testTaskNames = project.tasks.mapNotNull { task ->
            task.takeIf {
                it.name.endsWith("Test") && it.javaClass.name.contains("Test") &&
                    !it.javaClass.name.contains("Report")
            }?.run { "##TEST($name)##" }
        }

        doLast {
            println(testTaskNames.joinToString("\n"))
        }
    }

    val includeTestsMatchingPattern = project.findProperty("local.includeTestsMatching") as? String
    withType<Test>().configureEach {
        if (includeTestsMatchingPattern != null) filter.includeTestsMatching(includeTestsMatchingPattern)
        testLogging { showStandardStreams = true }
    }
    withType<KotlinJsTest>().configureEach {
        if (includeTestsMatchingPattern != null) filter.includeTestsMatching(includeTestsMatchingPattern)
        testLogging { showStandardStreams = true }
    }
    withType<KotlinNativeTest>().configureEach {
        if (includeTestsMatchingPattern != null) filter.includeTestsMatching(includeTestsMatchingPattern)
        testLogging { showStandardStreams = true }
    }
}

// WORKAROUND https://youtrack.jetbrains.com/issue/KT-79811/KJS-kotlinUpgradePackageLock-task-is-unreliable
fun BaseNpmExtension.configurePackageLockReports() {
    if (System.getenv("CI") != null) {
        packageLockMismatchReport.set(LockFileMismatchReport.NONE)
        packageLockAutoReplace.set(true)
    }
}

plugins.withType(NodeJsRootPlugin::class.java) {
    extensions.findByType(NpmExtension::class.java)?.apply {
        configurePackageLockReports()
    }
}

plugins.withType(WasmNodeJsRootPlugin::class.java) {
    extensions.findByType(WasmNpmExtension::class.java)?.apply {
        configurePackageLockReports()
    }
}
