@file:OptIn(ExperimentalPathApi::class)

package com.example

import de.infix.testBalloon.framework.core.TestSuiteScope
import de.infix.testBalloon.framework.core.testPlatform
import de.infix.testBalloon.framework.core.testSuite
import java.nio.file.Files
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.deleteRecursively
import kotlin.io.path.div
import kotlin.io.path.writeText

val EffectiveTestingJvm by testSuite {
// --8<-- [start:temporary-directory-per-test]
    testSuite("temporary directory per test") {
        temporaryDirectoryFixture().asParameterForEach {
            test("one") { directory ->
                (directory / "my-result1.txt").writeText("one")
            }

            test("two") { directory ->
                (directory / "my-result2.txt").writeText("two")
            }
        }
    }
// --8<-- [end:temporary-directory-per-test]

// --8<-- [start:temporary-directory-per-suite]
    testSuite("temporary directory per suite (invokable val)") {
        val directory = temporaryDirectoryFixture()

        test("one") {
            (directory() / "my-result1.txt").writeText("one")
        }

        test("two") {
            (directory() / "my-result2.txt").writeText("two")
        }
    }
// --8<-- [end:temporary-directory-per-suite]
}

// --8<-- [start:temporary-directory-fixture]
fun TestSuiteScope.temporaryDirectoryFixture(
    prefix: String = "${testSuiteInScope.testElementPath}-" // (1)!
) = testFixture {
    Files.createTempDirectory(Path("build/tmp"), prefix) // (2)!
} closeWith { testsSucceeded ->
    if (testsSucceeded || testPlatform.environment("CI") != null) {
        deleteRecursively()
    } else {
        println("Temporary directory: file://${toAbsolutePath()}") // (3)!
    }
}
// --8<-- [end:temporary-directory-fixture]
