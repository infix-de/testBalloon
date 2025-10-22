import buildLogic.addTestBalloonPluginFromProject
import org.apache.tools.ant.taskdefs.condition.Os

plugins {
    id("buildLogic.jvm")
}

addTestBalloonPluginFromProject(projects.testBalloonCompilerPlugin, projects.testBalloonFrameworkAbstractions)

dependencies {
    testImplementation(projects.testBalloonFrameworkCore)
    testImplementation(kotlin("test")) // for assertions only
}

tasks {
    val integrationTestRepositoryDir = rootProject.layout.buildDirectory.dir("integration-test-repository")
    val projectSourceTemplatesDirectory = project.layout.projectDirectory.dir("projectTemplates")
    val projectBuildTemplatesDirectory = project.layout.buildDirectory.dir("projectTemplates")

    val updateIntegrationTestRepository by registering(Exec::class) {
        group = "verification"
        description = "Updates the project's artifacts in the integration test repository."

        outputs.dir(integrationTestRepositoryDir)
        outputs.upToDateWhen { false }

        workingDir = rootDir
        commandLine = if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            mutableListOf("cmd", "/c", "gradlew.bat", "publishAllPublicationsToIntegrationTestRepository")
        } else {
            mutableListOf("./gradlew", "publishAllPublicationsToIntegrationTestRepository")
        }
    }

    val copyScenarioBuildGradleConfigurationToBuildTemplates by registering(Copy::class) {
        group = "verification"
        description = "Copies the root project's Gradle configuration into the project build templates directory."

        val primaryProjectDirectory = rootDir

        from(primaryProjectDirectory)
        into(projectBuildTemplatesDirectory.map { it.dir("common") })
        include("gradlew*", "gradle/**")
        include("kotlin-js-store/**")
    }

    val copyAndParameterizeScenarioSourceTemplatesToBuildTemplates by registering(Copy::class) {
        group = "verification"
        description = "Copies the projects' template sources into the project build templates directory," +
            " resolving template parameters."

        val baseVersions = with(project.the<VersionCatalogsExtension>().named("libs")) {
            versionAliases.associateWith { findVersion(it).get().displayName }
        }
        val basePluginVersions = with(project.the<VersionCatalogsExtension>().named("libs")) {
            pluginAliases.associateWith { findPlugin(it).get().get().version.toString() }
        }
        val baseProperties = project.properties.map { it.key!! to it.value.toString() }.toMap()
        val parameterRegex = Regex("""\{\{(.*?)\}\}""")

        inputs.property("baseVersions", baseVersions)
        inputs.property("basePluginVersions", basePluginVersions)
        inputs.property("baseProperties", baseProperties)

        from(projectSourceTemplatesDirectory)
        into(projectBuildTemplatesDirectory)
        filter { line ->
            line.replace(parameterRegex) { matchResult ->
                val (protocol, name) = matchResult.groupValues[1].split(':')
                when (protocol) {
                    "version" -> baseVersions[name] ?: "??version alias '$name' not found??"
                    "pluginVersion" -> basePluginVersions[name] ?: "??plugin alias '$name' not found??"
                    "prop" -> baseProperties[name] ?: "??property '$name' not found??"
                    "path" -> when (name) {
                        "integration-test-repository" -> integrationTestRepositoryDir.get().toString()
                        else -> "??unknown path name '$name'??"
                    }

                    else -> matchResult.value
                }
            }
        }
        filteringCharset = "UTF-8"
    }

    named("test") {
        inputs.files(updateIntegrationTestRepository)
        inputs.files(copyScenarioBuildGradleConfigurationToBuildTemplates)
        inputs.files(copyAndParameterizeScenarioSourceTemplatesToBuildTemplates)
    }
}
