package de.infix.testBalloon.framework.core

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.currentCoroutineContext
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TestConfigTests {
    @Test
    fun coroutineContext() = withTestFramework {
        val testSuite by testSuite("testSuite") {}

        TestConfig.executeWrapped(testSuite) {
            assertNull(currentCoroutineContext()[CoroutineName.Key])
        }

        val coroutineNameElement = CoroutineName("TEST-CC")
        TestConfig.coroutineContext(coroutineNameElement).executeWrapped(testSuite) {
            assertEquals(coroutineNameElement, currentCoroutineContext()[CoroutineName.Key])
        }
    }

    @Test
    fun invocation() = withTestFramework {
        val testSuite by testSuite("testSuite") {}

        TestConfig.executeWrapped(testSuite) {
            assertEquals(TestInvocation.SEQUENTIAL, TestInvocation.current())
        }

        TestConfig.invocation(TestInvocation.CONCURRENT).executeWrapped(testSuite) {
            assertEquals(TestInvocation.CONCURRENT, TestInvocation.current())
        }
    }

    @Test
    fun concurrencyAndTestScope() = withTestFramework {
        val testSuite by testSuite("testSuite") {}

        TestSession.DefaultConfiguration.executeWrapped(testSuite) {
            assertEquals(TestInvocation.SEQUENTIAL, TestInvocation.current())
            assertNotNull(TestScopeContext.current())
        }

        TestSession.DefaultConfiguration.invocation(TestInvocation.CONCURRENT).executeWrapped(testSuite) {
            assertEquals(TestInvocation.CONCURRENT, TestInvocation.current())
            assertNull(TestScopeContext.current())
        }

        assertFailsWith<IllegalArgumentException> {
            TestConfig.invocation(TestInvocation.CONCURRENT).testScope(isEnabled = true).executeWrapped(testSuite) {}
        }.also {
            assertContains(it.message!!, "attempt was made to enable a 'TestScope' in combination with concurrent")
        }

        val testConfig = TestConfig.invocation(TestInvocation.CONCURRENT)
            .addPermits(TestPermit.TEST_SCOPE_WITH_CONCURRENT_INVOCATION)
            .testScope(isEnabled = true)
        testConfig.parameterize(testSuite) // This is required to pick up permits
        testConfig.executeWrapped(testSuite) {
            assertEquals(TestInvocation.CONCURRENT, TestInvocation.current())
            assertNotNull(TestScopeContext.current())
        }
    }

    @Test
    fun permits() {
        testPermits(testConfig = TestConfig)
        testPermits(
            testConfig = TestConfig.permits(TestPermit.SUITE_WITHOUT_CHILDREN),
            TestPermit.SUITE_WITHOUT_CHILDREN
        )
        testPermits(
            testConfig = TestConfig
                .permits(TestPermit.SUITE_WITHOUT_CHILDREN, TestPermit.WRAPPER_WITHOUT_INNER_INVOCATION),
            TestPermit.SUITE_WITHOUT_CHILDREN,
            TestPermit.WRAPPER_WITHOUT_INNER_INVOCATION
        )
    }

    @Test
    fun permitsReplaced() {
        testPermits(
            testConfig = TestConfig
                .permits(TestPermit.SUITE_WITHOUT_CHILDREN)
                .permits(TestPermit.SUITE_WITHOUT_CHILDREN, TestPermit.WRAPPER_WITHOUT_INNER_INVOCATION),
            TestPermit.SUITE_WITHOUT_CHILDREN,
            TestPermit.WRAPPER_WITHOUT_INNER_INVOCATION
        )
    }

    @Test
    fun permitsAdded() {
        testPermits(
            testConfig = TestConfig
                .permits(TestPermit.SUITE_WITHOUT_CHILDREN)
                .addPermits(TestPermit.WRAPPER_WITHOUT_INNER_INVOCATION),
            TestPermit.SUITE_WITHOUT_CHILDREN,
            TestPermit.WRAPPER_WITHOUT_INNER_INVOCATION
        )
    }

    @Test
    fun permitsRemoved() {
        testPermits(
            testConfig = TestConfig
                .permits(TestPermit.SUITE_WITHOUT_CHILDREN, TestPermit.WRAPPER_WITHOUT_INNER_INVOCATION)
                .removePermits(TestPermit.WRAPPER_WITHOUT_INNER_INVOCATION),
            TestPermit.SUITE_WITHOUT_CHILDREN
        )
    }

    private fun testPermits(testConfig: TestConfig, vararg expectedPermits: TestPermit) = withTestFramework {
        val testSuite by testSuite("testSuite") {}

        testConfig.parameterize(testSuite)

        TestConfig.executeWrapped(testSuite) {
            assertContentEquals(parameters.permits.toList(), expectedPermits.toSet().toList())
        }
    }

    @Test
    fun testScope() = withTestFramework {
        val testSuite by testSuite("testSuite") {}

        TestConfig.executeWrapped(testSuite) {
            assertNull(TestScopeContext.current())
        }

        val testConfigWithTestScope = TestConfig.testScope(isEnabled = true)
        testConfigWithTestScope.executeWrapped(testSuite) {
            assertNotNull(TestScopeContext.current())
            testConfigWithTestScope.testScope(isEnabled = false).executeWrapped(testSuite) {
                assertNull(TestScopeContext.current())
            }
        }
    }

    @Test
    fun inheritance() = withTestFramework {
        val testSuite by testSuite("testSuite") {}

        val coroutineNameElement = CoroutineName("TEST-CC")
        TestConfig.coroutineContext(coroutineNameElement).executeWrapped(testSuite) {
            assertEquals(coroutineNameElement, currentCoroutineContext()[CoroutineName.Key])
            assertEquals(TestInvocation.SEQUENTIAL, TestInvocation.current())
            TestConfig.invocation(TestInvocation.CONCURRENT).executeWrapped(testSuite) {
                assertEquals(coroutineNameElement, currentCoroutineContext()[CoroutineName.Key])
                assertEquals(TestInvocation.CONCURRENT, TestInvocation.current())
            }
        }
    }

    @Test
    fun chaining() = withTestFramework {
        val testSuite by testSuite("testSuite") {}

        val coroutineNameElement = CoroutineName("TEST-CC")
        assertNull(currentCoroutineContext()[CoroutineName.Key])
        assertEquals(TestInvocation.SEQUENTIAL, TestInvocation.current())
        TestConfig
            .coroutineContext(coroutineNameElement)
            .invocation(TestInvocation.CONCURRENT).executeWrapped(testSuite) {
                assertEquals(coroutineNameElement, currentCoroutineContext()[CoroutineName.Key])
                assertEquals(TestInvocation.CONCURRENT, TestInvocation.current())
            }
    }
}
