import buildLogic.gradleRunCommandLine
import buildLogic.integrationTestRepository
import buildLogic.versionFromCatalog
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    id("buildLogic.kotlin-jvm")
    id("de.infix.testBalloon")
    id("com.github.gmazzo.buildconfig")
}

dependencies {
    testImplementation(projects.testBalloonFrameworkCore)
    testImplementation(libs.org.jetbrains.kotlin.test) // for assertions only
}

val integrationTestRepositoryDir = integrationTestRepository()
val projectRootDirectory = rootProject.layout.projectDirectory

buildConfig {
    packageName("buildConfig")
    useKotlinOutput { internalVisibility = true }

    buildConfigField("PROJECT_VERSION", provider { "$version" })
    buildConfigField("PROJECT_INTEGRATION_TEST_REPOSITORY", integrationTestRepositoryDir.map { "$it" })
    buildConfigField("PROJECT_ROOT_DIRECTORY", projectRootDirectory.asFile)
    buildConfigField(
        "PROJECT_CATALOG_VERSIONS",
        with(project.the<VersionCatalogsExtension>().named("libs")) {
            versionAliases.associateWith { findVersion(it).get().displayName }
        }
    )
    buildConfigField(
        "KOTLIN_ALL_TEST_RELEASES",
        project.versionFromCatalog("org-jetbrains-kotlin-all-test-releases").split(';')
    )
}

tasks {
    val updateIntegrationTestRepository = register<Exec>("updateIntegrationTestRepository") {
        group = "verification"
        description = "Updates the project's artifacts in the integration test repository."

        outputs.dir(integrationTestRepositoryDir)
        outputs.upToDateWhen { false }

        workingDir = rootDir
        commandLine = gradleRunCommandLine("--warn", "publishAllPublicationsToIntegrationTestRepository")

        val integrationTestRepositoryDir = integrationTestRepositoryDir.get().asFile

        doFirst {
            integrationTestRepositoryDir.deleteRecursively()
        }
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
