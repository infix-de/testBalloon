@file:OptIn(ExperimentalPathApi::class)

package com.example

import de.infix.testBalloon.framework.core.TestSuiteScope
import de.infix.testBalloon.framework.core.testPlatform
import de.infix.testBalloon.framework.core.testSuite
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.deleteRecursively
import kotlin.io.path.div
import kotlin.io.path.writeText
import kotlin.test.assertEquals

val ExperimentalSuite by testSuite {
    testSuite("directory per test") {
        testDirectoryFixture().asParameterForEach {
            test("succeeds") { directory ->
                (directory / "my-result1.txt").writeText("Succeed")
                assertEquals(7, "Succeed".length)
            }

            test("fails") { directory ->
                (directory / "my-result2.txt").writeText("Fail")
                assertEquals(1, "Fail".length)
            }
        }
    }

    testSuite("directory per suite (as parameter)") {
        testDirectoryFixture().asParameterForAll {
            test("succeeds") { directory ->
                (directory / "my-result1.txt").writeText("Succeed")
                assertEquals(7, "Succeed".length)
            }

            test("fails") { directory ->
                (directory / "my-result2.txt").writeText("Fail")
                assertEquals(1, "Fail".length)
            }
        }
    }

    testSuite("directory per suite (invokable val)") {
        val directory = testDirectoryFixture()

        test("succeeds") {
            (directory() / "my-result1.txt").writeText("Succeed")
            assertEquals(7, "Succeed".length)
        }

        test("fails") {
            (directory() / "my-result2.txt").writeText("Fail")
            assertEquals(1, "Fail".length)
        }
    }
}

fun TestSuiteScope.testDirectoryFixture(
    prefix: String = "${testSuiteInScope.testElementPath}-",
    baseDirectory: Path = Path("build/tmp")
) = testFixture {
    Files.createTempDirectory(baseDirectory, prefix)
} closeWith { testsSucceeded ->
    if (testsSucceeded || testPlatform.environment("CI") != null) {
        deleteRecursively()
    } else {
        println("Temporary directory: file://${toAbsolutePath()}")
    }
}
