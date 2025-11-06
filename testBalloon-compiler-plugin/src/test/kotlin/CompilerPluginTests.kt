@file:OptIn(TestBalloonInternalApi::class)

import buildConfig.BuildConfig.PROJECT_COMPILER_PLUGIN_ID
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.PluginOption
import com.tschuchort.compiletesting.SourceFile
import de.infix.testBalloon.compilerPlugin.CompilerPluginCommandLineProcessor
import de.infix.testBalloon.compilerPlugin.CompilerPluginRegistrar
import de.infix.testBalloon.framework.shared.internal.Constants
import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.incremental.deleteRecursivelyOrThrow
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.io.PrintStream
import kotlin.coroutines.Continuation
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCompilerApi::class)
private class CompilerPluginTests {
    @Test
    fun suiteDiscovery() {
        listOf("com.example", "").forEach { packageName ->
            println("=== packageName='$packageName' ===")

            val packageDeclaration = if (packageName.isEmpty()) "" else "package $packageName"

            compilation(
                """
                    $packageDeclaration
                    
                    import fakeTestFramework.TestSuite
                    import fakeTestFramework.testSuite

                    val TestSuiteOne by testSuite {}

                    class TestSuiteTwo : TestSuite(content = {})
                """,
                debugLevel = "DISCOVERY"
            ) {
                val packageNameDot = if (packageName.isEmpty()) "" else "$packageName."

                assertTrue("Found top-level test suite property '${packageNameDot}TestSuiteOne'" in messages)
                assertTrue("Found top-level test suite class '${packageNameDot}TestSuiteTwo'" in messages)
            }
        }
    }

    @Test
    fun initialization() {
        val packageName = "com.example"

        val d = "$"

        compilation(
            """
                package $packageName
                
                import fakeTestFramework.TestSession
                import fakeTestFramework.TestSuite
                import fakeTestFramework.testSuite

                val TestSuiteOne by testSuite {
                    println("$d{testElementPath}")
                }
                
                class MyTestSession : TestSession() {
                    init {
                        println("$d{this::class.qualifiedName}")
                    }
                }

                class TestSuiteTwo : TestSuite(content = {
                    println("$d{testElementPath}")
                })

                val testSuiteThree by testSuite("my test suite three") {
                    println("$d{testElementPath}")
                }
            """,
            debugLevel = "DISCOVERY",
            executionEnabled = true
        ) { capturedStdout ->

            assertContains(
                capturedStdout,
                """
                    $packageName.MyTestSession
                    $packageName.TestSuiteOne
                    $packageName.TestSuiteTwo
                    my test suite three
                """.trimIndent()
            )
        }
    }

    @Test
    fun insistOnSingleTestSession() {
        compilation(
            """
                package com.example
                
                import fakeTestFramework.TestSession

                class MyTestSession : TestSession()
                class MyOtherTestSession : TestSession()
            """,
            expectedExitCode = KotlinCompilation.ExitCode.COMPILATION_ERROR
        ) {
            assertTrue("Found multiple test sessions annotated with @TestRegistering" in messages)
        }
    }

    @Test
    fun debugEnabled() {
        compilation(
            """
                import fakeTestFramework.testSuite
                
                val TestSuiteOne by testSuite {}
            """,
            debugLevel = "DISCOVERY"
        ) {
            assertTrue("[DEBUG] Found top-level test suite property 'TestSuiteOne'" in messages)
        }
    }

    @Test
    fun disableForNonTestModule() {
        compilation(
            """
                val foo = 1
            """,
            isTestModule = false,
            classPathInheritanceEnabled = false,
            debugLevel = "BASIC"
        ) {
            assertTrue("[DEBUG] Disabling the plugin for module <module>: It is not a test module." in messages)
        }
    }

    @Test
    fun disableOnMissingFrameworkLibraryDependency() {
        compilation(
            """
                val foo = 1
            """,
            classPathInheritanceEnabled = false,
            debugLevel = "BASIC"
        ) {
            assertTrue(
                "[DEBUG] Disabling the plugin for module <module_test>: It has no framework library dependency." in
                    messages
            )
        }
    }

    @Test
    fun defectiveFrameworkLibraryDependency() {
        compilation(
            """
                package ${Constants.SHARED_PACKAGE_NAME}
                interface AbstractTestSuite
                val foo = 1
            """,
            classPathInheritanceEnabled = false,
            expectedExitCode = KotlinCompilation.ExitCode.COMPILATION_ERROR
        ) {
            assertTrue("Please add the dependency '" in messages)
        }
    }
}

@OptIn(ExperimentalCompilerApi::class)
private fun compilation(
    sourceCode: String,
    isTestModule: Boolean = true,
    debugLevel: String? = null,
    executionEnabled: Boolean = false,
    classPathInheritanceEnabled: Boolean = true,
    expectedExitCode: KotlinCompilation.ExitCode = KotlinCompilation.ExitCode.OK,
    action: JvmCompilationResult.(capturedStdout: String) -> Unit
) {
    val compilation = KotlinCompilation()

    fun option(name: String, value: String): PluginOption = PluginOption(PROJECT_COMPILER_PLUGIN_ID, name, value)

    try {
        compilation.apply {
            moduleName = if (isTestModule) "module_test" else "module"
            sources = listOf(
                SourceFile.kotlin("Main.kt", sourceCode.trimIndent()),
                SourceFile.kotlin(
                    Constants.ENTRY_POINT_ANCHOR_FILE_NAME,
                    """
                        package ${Constants.ENTRY_POINT_PACKAGE_NAME}
                        // In real-world use cases, this file is generated by the Gradle plugin.
                        // The compiler plugin will populate it with entry point code. 
                    """.trimIndent()
                )
            )
            verbose = false
            compilerPluginRegistrars = listOf(CompilerPluginRegistrar())
            inheritClassPath = classPathInheritanceEnabled
            commandLineProcessors = listOf(CompilerPluginCommandLineProcessor())
            pluginOptions = listOfNotNull(
                if (debugLevel != null) option("debugLevel", debugLevel) else null,
                if (executionEnabled) option("jvmStandalone", "true") else null
            )
            messageOutputStream = OutputStream.nullOutputStream()
        }.compile().run {
            println("--- Compilation ---")
            println(messages)

            assertEquals(expectedExitCode, exitCode, messages)

            var capturedStdout = ""

            if (executionEnabled) {
                val entryPointClass = classLoader.loadClass(Constants.ENTRY_POINT_ANCHOR_CLASS_NAME)
                capturedStdout = capturedStdout {
                    runBlocking {
                        entryPointClass.getDeclaredMethod(
                            "main",
                            Array<String>::class.java,
                            Continuation::class.java
                        ).invoke(
                            entryPointClass,
                            null,
                            Continuation<Unit>(currentCoroutineContext()) {}
                        )
                    }
                }

                println("--- Execution ---")
                println(capturedStdout)
            }

            action(capturedStdout)
        }
    } finally {
        compilation.workingDir.deleteRecursivelyOrThrow()
    }
}

private fun capturedStdout(action: () -> Unit): String {
    val originalStdout = System.out
    val stdoutCapturingStream = ByteArrayOutputStream()
    System.setOut(PrintStream(stdoutCapturingStream))

    action()

    System.setOut(originalStdout)

    // Return a string with normalized line separators.
    return stdoutCapturingStream.toString().lines().joinToString("\n")
}
