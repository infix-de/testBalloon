package buildLogic

import nmcp.NmcpAggregationExtension
import nmcp.NmcpExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Exec
import java.time.temporal.ChronoUnit

@Suppress("unused")
class BuildLogicAggregationPublishingPlugin : Plugin<Project> {
    @Suppress("NewApi")
    override fun apply(target: Project): Unit = with(target) {
        with(pluginManager) {
            apply("maven-publish")
            apply("com.gradleup.nmcp")
            apply("com.gradleup.nmcp.aggregation")
        }

        if (projectDir != rootDir) {
            throw IllegalArgumentException("Please apply this plugin only to the root project")
        }

        val stagingRepository = project.layout.buildDirectory.dir("aggregate-staging-repository")

        val populateAggregationStagingRepository =
            tasks.register("populateAggregationStagingRepository", Exec::class.java) {
                group = "publishing"
                description = "Populate the aggregation staging directory with publishable artifacts."

                outputs.dir(stagingRepository)
                outputs.upToDateWhen { false }

                commandLine = gradleRunCommandLine("--warn", "publishAllPublicationsToAggregationStagingRepository")

                doFirst {
                    stagingRepository.get().asFile.deleteRecursively()
                }
            }

        extensions.getByType(NmcpExtension::class.java).apply {
            extraFiles(populateAggregationStagingRepository.map { it.outputs })
        }

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
