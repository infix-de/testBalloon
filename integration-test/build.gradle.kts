import buildLogic.addTestBalloonPluginFromProject
import buildLogic.gradleRunCommandLine
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    id("buildLogic.kotlin-jvm")
    id("com.github.gmazzo.buildconfig")
}

addTestBalloonPluginFromProject(projects.testBalloonCompilerPlugin)

dependencies {
    testImplementation(projects.testBalloonFrameworkCore)
    testImplementation(libs.org.jetbrains.kotlin.test) // for assertions only
}

val integrationTestRepositoryDir = rootProject.layout.buildDirectory.dir("integration-test-repository")
val projectRootDirectory = rootProject.layout.projectDirectory

buildConfig {
    packageName("buildConfig")
    useKotlinOutput { internalVisibility = true }

    buildConfigField("PROJECT_VERSION", provider { "${project.version}" })
    buildConfigField("PROJECT_INTEGRATION_TEST_REPOSITORY", integrationTestRepositoryDir.map { "$it" })
    buildConfigField("PROJECT_ROOT_DIRECTORY", projectRootDirectory.asFile)
    buildConfigField(
        "PROJECT_CATALOG_VERSIONS",
        with(project.the<VersionCatalogsExtension>().named("libs")) {
            versionAliases.associateWith { findVersion(it).get().displayName }
        }
    )
}

tasks {
    val updateIntegrationTestRepository by registering(Exec::class) {
        group = "verification"
        description = "Updates the project's artifacts in the integration test repository."

        outputs.dir(integrationTestRepositoryDir)
        outputs.upToDateWhen { false }

        workingDir = rootDir
        commandLine = gradleRunCommandLine("--warn", "publishAllPublicationsToIntegrationTestRepository")
    }

    withType(Test::class) {
        inputs.files(updateIntegrationTestRepository)

        testLogging {
            showStandardStreams = true
            showStackTraces = true
            exceptionFormat = TestExceptionFormat.FULL
        }
    }
}
