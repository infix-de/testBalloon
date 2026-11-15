import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

class CompilerPluginTests {
    @Test
    fun initialization() = withGradleRun(
        $$"""
            import de.infix.testBalloon.framework.core.TestSession
            import de.infix.testBalloon.framework.core.TestSuite
            import de.infix.testBalloon.framework.core.testSuite

            val TestSuiteOne by testSuite {
                test("test") { log("$testElementPath") }
            }
            
            class MyTestSession : TestSession() {
                init { log(this::class.qualifiedName) }
            }

            val testSuiteTwo by testSuite("my test suite two") {
                test("test") { log("$testElementPath") }
            }
            
            fun log(message: String?) {
                println("##LOG($message)LOG##")
            }
        """,
        gradleArguments = listOf("test"),
        debugLevel = "DISCOVERY"
    ) {
        listOf(
            "test.MyTestSession",
            "«test.TestSuiteOne↘test»",
            "«test.testSuiteTwo↘test»"
        ).forEach { item ->
            assertContains(normalizedOutput, "##LOG($item)LOG##")
        }
    }

    @Test
    fun insistOnSingleTestSession() = withGradleRun(
        """
            import de.infix.testBalloon.framework.core.TestSession

            class MyTestSession : TestSession()
            class MyOtherTestSession : TestSession()
        """,
        expectSuccess = false
    ) {
        assertTrue("Found multiple test sessions annotated with @TestRegistering" in normalizedOutput)
    }

    @Test
    fun topLevelSuiteVisibility() = withGradleRun(
        """
            import de.infix.testBalloon.framework.core.testSuite
            
            private val TestSuiteOne by testSuite {}
        """,
        expectSuccess = false
    ) {
        assertTrue("Top-level test suite property must have 'internal' or 'public' visibility." in normalizedOutput)
    }

    @Test
    fun topLevelSuiteWithArgumentReordering() = withGradleRun(
        """
            import de.infix.testBalloon.framework.shared.TestRegistering
            import de.infix.testBalloon.framework.shared.TestSuitePropertyName
            import de.infix.testBalloon.framework.core.testSuite
            import de.infix.testBalloon.framework.core.TestSuite

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
        assertTrue("[DEBUG] Found top-level test suite property 'test.SuiteWithArgumentReordering'" in normalizedOutput)
    }

    @Test
    fun discoveryDebugLogging() = withGradleRun(
        """
            import de.infix.testBalloon.framework.core.testSuite
            
            val TestSuiteOne by testSuite {}
        """,
        debugLevel = "DISCOVERY"
    ) {
        assertTrue("[DEBUG] Found top-level test suite property 'test.TestSuiteOne'" in normalizedOutput)
    }

    @Test
    fun defectiveFrameworkLibraryDependency() = withGradleRun(
        """
            val foo = 1
        """,
        frameworkDependencyEnabled = false,
        expectSuccess = false
    ) {
        assertTrue("Please add the dependency '" in normalizedOutput)
    }
}
