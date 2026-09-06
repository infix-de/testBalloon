package de.infix.testBalloon.gradlePlugin.internal.layer.kotlin2420RC

import de.infix.testBalloon.framework.shared.internal.Constants
import de.infix.testBalloon.gradlePlugin.TestBalloonGradleExtension
import de.infix.testBalloon.gradlePlugin.internal.layer.kotlin2320.TestBalloonGradlePlugin_2_3_20
import de.infix.testBalloon.gradlePlugin.internal.testBalloonEnvironment
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.jetbrains.kotlin.gradle.DelicateKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalJsTestDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBrowserTestDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTestsLocation
import java.io.File
import java.net.URI
import java.net.URLEncoder

@Suppress("unused", "ClassName")
class TestBalloonGradlePlugin_2_4_20_RC : TestBalloonGradlePlugin_2_3_20()

/**
 * Enables TestBalloon for Playwright-based browser tests.
 *
 * Note: This function cannot be used in build scripts for Gradle versions whose embedded Kotlin does not have context
 * parameters enabled by default. Gradle does not provide a mechanism to configure Kotlin compiler options for build
 * scripts. See https://github.com/gradle/gradle/issues/24221.
 */
@OptIn(ExperimentalJsTestDsl::class)
context(project: Project)
fun KotlinJsBrowserTestDsl.withTestBalloon() {
    @OptIn(DelicateKotlinGradlePluginApi::class)
    testsLocation.set(
        defaultTestsLocationProvider.map {
            TestBalloonKotlinJsTestLocations(it, project)
        }
    )

    project.extensions.configure<TestBalloonGradleExtension>(Constants.GRADLE_EXTENSION_NAME) {
        browserAutoIntegrationEnabled = false
    }
}

/**
 * Enables TestBalloon for Playwright-based browser tests.
 *
 * Note: This function must be used in build scripts for Gradle versions whose embedded Kotlin does not have context
 * parameters enabled by default.
 */
@OptIn(ExperimentalJsTestDsl::class)
@Suppress("unused")
fun KotlinJsBrowserTestDsl.withTestBalloon(project: Project) = with(project) {
    withTestBalloon()
}

@OptIn(ExperimentalJsTestDsl::class, DelicateKotlinGradlePluginApi::class)
private class TestBalloonKotlinJsTestLocations(private val base: KotlinJsTestsLocation, project: Project) :
    KotlinJsTestsLocation {
    @get:InputDirectory
    override val bundleLocation: Provider<Directory> get() = base.bundleLocation

    private var testHtmlFile: File? = null

    // TODO: insert include/exclude patterns
    private val testBalloonEnvironment = project.testBalloonEnvironment(
        secondaryIncludePatterns = emptyList(),
        secondaryExcludePatterns = emptyList()
    )

    @get:Input
    override val testHtmlFileName: Provider<String>
        get() =
            base.testHtmlFileName.map { fileName ->
                if (testHtmlFile == null) { // Avoid creating the file more than once.
                    testHtmlFile = File("${bundleLocation.get()}/$fileName").apply {
                        writeText(htmlFileContent)
                    }
                }
                fileName
            }

    @get:Internal
    override val url: Provider<URI>
        get() = base.url.map {
            // TODO: escape quotes in environment names and values
            val testBalloonEnvironmentValue = URLEncoder.encode(
                "{${testBalloonEnvironment.map { (name, value) -> "\"$name\": \"$value\"" }.joinToString(",")}}",
                "UTF-8"
            )
            URI("$it?testBalloonEnvironment=$testBalloonEnvironmentValue")
        }
}

@Suppress("ktlint:standard:property-naming")
private val htmlFileContent = """
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="utf-8">
        <title>Kotlin JS Tests via TestBalloon</title>
        <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    </head>
    <body>
    <script>
        const urlParams = new URLSearchParams(window.location.search);
        window.kotlinTestConfig = JSON.parse(urlParams.get('kotlinTestConfig') || '{}');
        window.testBalloonEnvironment = JSON.parse(urlParams.get('testBalloonEnvironment') || '{}');
    </script>
    <script src="tests.bundle.js"></script>
    </body>
    </html>
""".trimIndent()
