package de.infix.testBalloon.framework.internal

import de.infix.testBalloon.framework.TestConfig
import de.infix.testBalloon.framework.TestElement
import de.infix.testBalloon.framework.assertMessageStartsWith
import de.infix.testBalloon.framework.disable
import de.infix.testBalloon.framework.reference
import de.infix.testBalloon.framework.shared.internal.Constants
import de.infix.testBalloon.framework.testSuite
import de.infix.testBalloon.framework.withTestFramework
import de.infix.testBalloon.framework.withTestReport
import kotlinx.coroutines.test.TestResult
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

@Suppress("SpellCheckingInspection")
class TestFrameworkTests {
    private val iSep = Constants.INTERNAL_PATH_ELEMENT_SEPARATOR

    @Test
    fun frameworkNotInitialized() {
        assertFailsWith<IllegalStateException> {
            val suite by testSuite("default configuration") {}
            suite.reference()
        }.assertMessageStartsWith("The test framework was not initialized.")
    }

    @Test
    fun elementSelectionByArguments() = verifyElementSelection(
        ArgumentsBasedElementSelection(arrayOf("--include", "suite1|sub-suite1|*")),
        listOf(
            Pair("«suite1${iSep}sub-suite1${iSep}test1»", true),
            Pair("«suite1${iSep}sub-suite1${iSep}test2»", false)
        )
    )

    @Test
    fun elementSelectionByEnvironment() = verifyElementSelection(
        EnvironmentBasedElementSelection(includePatterns = "suite1|sub-suite1|*", excludePatterns = null),
        listOf(
            Pair("«suite1${iSep}sub-suite1${iSep}test1»", true),
            Pair("«suite1${iSep}sub-suite1${iSep}test2»", false)
        )
    )

    @Test
    fun elementSelectionWithCustomSeparator() = verifyElementSelection(
        EnvironmentBasedElementSelection(includePatterns = ";suite1;sub-suite1;te*1", excludePatterns = null),
        listOf(
            Pair("«suite1${iSep}sub-suite1${iSep}test1»", true)
        )
    )

    @Test
    fun elementSelectionAllIn() = verifyElementSelection(
        TestElement.AllInSelection,
        listOf(
            Pair("«suite1${iSep}test1»", true),
            Pair("«suite1${iSep}test2»", false),
            Pair("«suite1${iSep}sub-suite1${iSep}test1»", true),
            Pair("«suite1${iSep}sub-suite1${iSep}test2»", false),
            Pair("«suite1${iSep}sub-suite2${iSep}test1»", false),
            Pair("«suite1${iSep}sub-suite2${iSep}test2»", false),
            Pair("«suite2${iSep}test1»", true),
            Pair("«suite2${iSep}test2»", false)
        )
    )

    @Test
    fun elementSelectionWithShortIncludePrefix() = verifyElementSelection(
        EnvironmentBasedElementSelection(includePatterns = "s*uite1|sub-suite1|*", excludePatterns = null),
        listOf(
            Pair("«suite1${iSep}sub-suite1${iSep}test1»", true),
            Pair("«suite1${iSep}sub-suite1${iSep}test2»", false)
        )
    )

    @Test
    fun elementSelectionWithExclusion() = verifyElementSelection(
        EnvironmentBasedElementSelection(includePatterns = null, excludePatterns = "*|test1"),
        listOf(
            Pair("«suite1${iSep}test2»", false),
            Pair("«suite1${iSep}sub-suite1${iSep}test2»", false),
            Pair("«suite1${iSep}sub-suite2${iSep}test2»", false),
            Pair("«suite2${iSep}test2»", false)
        )
    )

    @Test
    fun elementSelectionWithInclusionAndExclusion() = verifyElementSelection(
        EnvironmentBasedElementSelection(
            includePatterns = "suite1|*",
            excludePatterns = "*|sub-suite1|*"
        ),
        listOf(
            Pair("«suite1${iSep}test1»", true),
            Pair("«suite1${iSep}test2»", false),
            Pair("«suite1${iSep}sub-suite2${iSep}test1»", false),
            Pair("«suite1${iSep}sub-suite2${iSep}test2»", false)
        )
    )

    private fun verifyElementSelection(
        selection: TestElement.Selection,
        expectedResult: List<Pair<String, Boolean>>
    ): TestResult = withTestFramework {
        val suite1 by testSuite("suite1") {
            test("test1") {}
            test("test2", testConfig = TestConfig.disable()) {}

            testSuite("sub-suite1") {
                test("test1") {}
                test("test2", testConfig = TestConfig.disable()) {}
            }

            testSuite("sub-suite2", testConfig = TestConfig.disable()) {
                test("test1") {}
                test("test2", testConfig = TestConfig.disable()) {}
            }
        }

        val suite2 by testSuite("suite2") {
            test("test1") {}
            test("test2", testConfig = TestConfig.disable()) {}
        }

        withTestReport(suite1, suite2, selection = selection) {
            with(finishedTestEvents()) {
                assertContentEquals(
                    expectedResult,
                    map { Pair("${it.element.testElementPath}", it.element.testElementIsEnabled) }
                )
            }
        }
    }
}
