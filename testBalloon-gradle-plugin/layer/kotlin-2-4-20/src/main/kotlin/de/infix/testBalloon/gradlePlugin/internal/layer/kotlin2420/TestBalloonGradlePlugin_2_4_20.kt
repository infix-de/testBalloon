package de.infix.testBalloon.gradlePlugin.internal.layer.kotlin2420

import de.infix.testBalloon.framework.shared.internal.Constants
import de.infix.testBalloon.gradlePlugin.TestBalloonGradleExtension
import de.infix.testBalloon.gradlePlugin.internal.layer.kotlin2320.TestBalloonGradlePlugin_2_3_20
import de.infix.testBalloon.gradlePlugin.internal.testBalloonEnvironment
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.jetbrains.kotlin.gradle.DelicateKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalJsTestDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBrowserTestDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTestsLocation
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinWasmTargetDsl
import java.io.File
import java.net.URI
import java.net.URLEncoder

@Suppress("unused", "ClassName")
class TestBalloonGradlePlugin_2_4_20 : TestBalloonGradlePlugin_2_3_20()

/**
 * Enables TestBalloon for Playwright-based browser tests.
 *
 * Note: This function is provisional and will be replaced with autoconfiguration once the Kotlin Gradle plugin
 * is equipped: https://youtrack.jetbrains.com/issue/KT-89230
 */
@OptIn(ExperimentalJsTestDsl::class)
@Suppress("unused")
fun KotlinJsBrowserTestDsl.withTestBalloon(kotlinJsTargetDsl: KotlinJsTargetDsl) {
    @OptIn(DelicateKotlinGradlePluginApi::class)
    testsLocation.set(
        defaultTestsLocationProvider.map { baseTestsLocation ->
            TestBalloonKotlinJsTestLocations(baseTestsLocation, kotlinJsTargetDsl)
        }
    )

    return kotlinJsTargetDsl.project.extensions.configure<TestBalloonGradleExtension>(Constants.GRADLE_EXTENSION_NAME) {
        browserAutoIntegrationEnabled = false
    }
}

@OptIn(ExperimentalJsTestDsl::class, DelicateKotlinGradlePluginApi::class)
private class TestBalloonKotlinJsTestLocations(
    private val base: KotlinJsTestsLocation,
    kotlinJsTargetDsl: KotlinJsTargetDsl
) : KotlinJsTestsLocation {
    @get:InputDirectory
    override val bundleLocation: Provider<Directory> get() = base.bundleLocation

    private val outputModuleName: Provider<String> = kotlinJsTargetDsl.outputModuleName

    private val useWasmJsTarget: Boolean = (kotlinJsTargetDsl as? KotlinWasmTargetDsl)?.wasmTargetType != null

    private var testHtmlFile: File? = null

    // TODO: insert include/exclude patterns
    private val testBalloonEnvironment = kotlinJsTargetDsl.project.testBalloonEnvironment(
        secondaryIncludePatterns = emptyList(),
        secondaryExcludePatterns = emptyList()
    )

    @get:Input
    override val testHtmlFileName: Provider<String>
        get() =
            base.testHtmlFileName.map { fileName ->
                if (testHtmlFile == null) { // Avoid creating the file more than once.
                    testHtmlFile = File("${bundleLocation.get()}/$fileName").apply {
                        writeText(htmlFileContent(outputModuleName.get(), useWasmJsTarget))
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

private fun htmlFileContent(outputModuleName: String, useWasmJsTarget: Boolean): String {
    val targetName = if (useWasmJsTarget) "Wasm/JS" else "JS"

    val bootstrapScriptTags = if (useWasmJsTarget) {
        val testModule = """kotlin/$outputModuleName-test.mjs"""
        // language=HTML
        """
            <script type="module" src="$testModule"></script>
            <script>
                window.addEventListener("load", async function () {
                    try {
                        let module = await import("./$testModule");
                        module["runTestBalloonViaPlaywright"]();
                    } catch (exception) {
                        console.error("Failed to load TestBalloon Wasm/JS entry point:", exception.toString());
                    }
                });
            </script>
        """
    } else {
        """<script src="tests.bundle.js"></script>"""
    }

    // language=HTML
    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="utf-8">
            <title>$targetName Tests via TestBalloon</title>
            <meta name="viewport" content="width=device-width, initial-scale=1.0" />
        </head>
        <body>
        <script>
            const urlParams = new URLSearchParams(window.location.search);
            window.kotlinTestConfig = JSON.parse(urlParams.get("kotlinTestConfig") || "{}");
            window.testBalloonEnvironment = JSON.parse(urlParams.get("testBalloonEnvironment") || "{}");
        </script>
        $bootstrapScriptTags
        </body>
        </html>
    """.trimIndent()
}
