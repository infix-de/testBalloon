package buildLogic

import nmcp.NmcpAggregationExtension
import nmcp.NmcpExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.time.temporal.ChronoUnit

@Suppress("unused")
class BuildLogicAggregationPublishingPlugin : Plugin<Project> {
    @Suppress("NewApi")
    override fun apply(target: Project): Unit = with(target) {
        if (project != rootProject) {
            throw IllegalArgumentException("Please apply this plugin only to the root project instead of $project")
        }

        with(pluginManager) {
            apply("com.gradleup.nmcp")
            apply("com.gradleup.nmcp.aggregation")
            apply("maven-publish") // satisfy nmcp check
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
