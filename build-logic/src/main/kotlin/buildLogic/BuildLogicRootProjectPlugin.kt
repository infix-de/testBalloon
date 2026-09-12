package buildLogic

import nmcp.NmcpAggregationExtension
import nmcp.NmcpExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.BaseNpmExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.LockFileMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmExtension
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.npm.WasmNpmExtension
import java.time.temporal.ChronoUnit

@Suppress("unused")
class BuildLogicRootProjectPlugin : Plugin<Project> {
    @Suppress("NewApi")
    override fun apply(target: Project): Unit = with(target) {
        if (project != rootProject) {
            throw IllegalArgumentException("Please apply this plugin only to the root project instead of $project")
        }

        with(pluginManager) {
            apply("com.gradleup.nmcp")
            apply("com.gradleup.nmcp.aggregation")
            apply("maven-publish") // satisfy nmcp check
            apply(NpmExtensionSetupRootPlugin::class.java)
        }

        extensions.getByType(NmcpExtension::class.java).apply {
            extraFiles(project.files(aggregationStagingRepository()).singleFile)
        }

        dependencies.add("nmcpAggregation", dependencies.project(":")) // satisfy nmcp requirement

        extensions.getByType(NmcpAggregationExtension::class.java).apply {
            centralPortal {
                username.set(System.getenv("ORG_GRADLE_PROJECT_mavenCentralUsername"))
                password.set(System.getenv("ORG_GRADLE_PROJECT_mavenCentralPassword"))

                publishingType.set("USER_MANAGED")
                validationTimeout.set(java.time.Duration.of(30, ChronoUnit.MINUTES))
            }
        }
    }
}

private class NpmExtensionSetupRootPlugin : Plugin<Project> {
    // WORKAROUND https://youtrack.jetbrains.com/issue/KT-79811/KJS-kotlinUpgradePackageLock-task-is-unreliable

    override fun apply(target: Project): Unit = with(target) {
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
    }
}
