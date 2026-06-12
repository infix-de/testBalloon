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
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.incremental.deleteRecursivelyOrThrow
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.io.PrintStream
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCompilerApi::class)
private class CompilerPluginTests {
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

                val testSuiteTwo by testSuite("my test suite two") {
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
                    $packageName.testSuiteTwo
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
    fun topLevelSuiteVisibility() {
        compilation(
            """
                import fakeTestFramework.testSuite
                
                private val TestSuiteOne by testSuite {}
            """,
            expectedExitCode = KotlinCompilation.ExitCode.COMPILATION_ERROR
        ) {
            assertTrue("Top-level test suite property must have 'internal' or 'public' visibility." in messages)
        }
    }

    @Test
    fun topLevelSuiteWithArgumentReordering() {
        compilation(
            """
                import de.infix.testBalloon.framework.shared.TestRegistering
                import de.infix.testBalloon.framework.shared.TestSuitePropertyName
                import fakeTestFramework.testSuite
                import fakeTestFramework.TestSuite

                @TestRegistering
                fun customTestSuite(
                    @TestSuitePropertyName propertyName: String = "",
                    first: List<Int>? = null,
                    second: List<Int>? = null,
                    content: TestSuite.() -> Unit
                ): Lazy<TestSuite> = testSuite(qualifiedPropertyName = propertyName, content = content)
                
                val SuiteWithArgumentReordering by customTestSuite(
                    second = listOf(2), // declared 2nd, passed 1st  -> out of order
                    first = listOf(1) // declared 1st, passed 2nd  -> non-constant value forces a temporary
                ) {
                }
            """,
            debugLevel = "DISCOVERY"
        ) {
            assertTrue("[DEBUG] Found top-level test suite property 'SuiteWithArgumentReordering'" in messages)
        }
    }

    @Test
    fun discoveryDebugLogging() {
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
            moduleName = "module"
            sources = listOf(SourceFile.kotlin("Main.kt", sourceCode.trimIndent()))
            verbose = false
            compilerPluginRegistrars = listOf(CompilerPluginRegistrar())
            inheritClassPath = classPathInheritanceEnabled
            commandLineProcessors = listOf(CompilerPluginCommandLineProcessor())
            pluginOptions = listOfNotNull(
                if (debugLevel != null) option("debugLevel", debugLevel) else null
            )
            messageOutputStream = OutputStream.nullOutputStream()
            optIn = listOf()
        }.compile().run {
            println("--- Compilation ---")
            println(messages)

            assertEquals(expectedExitCode, exitCode, messages)

            var capturedStdout = ""

            if (executionEnabled) {
                val entryPointClass = classLoader.loadClass(Constants.JVM_ENTRY_POINT_CLASS_NAME)
                capturedStdout = capturedStdout {
                    entryPointClass.getDeclaredMethod(Constants.JVM_DISCOVERY_RESULT_METHOD_NAME).invoke(
                        entryPointClass
                    )
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
