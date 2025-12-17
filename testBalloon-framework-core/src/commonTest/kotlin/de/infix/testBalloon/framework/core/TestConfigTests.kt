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
            assertEquals(TestConfig.Invocation.Sequential, TestConfig.Invocation.current())
        }

        TestConfig.invocation(TestConfig.Invocation.Concurrent).executeWrapped(testSuite) {
            assertEquals(TestConfig.Invocation.Concurrent, TestConfig.Invocation.current())
        }
    }

    @Test
    fun concurrencyAndTestScope() = withTestFramework {
        val testSuite by testSuite("testSuite") {}

        TestSession.DefaultConfiguration.executeWrapped(testSuite) {
            assertEquals(TestConfig.Invocation.Sequential, TestConfig.Invocation.current())
            assertNotNull(TestScopeContext.current())
        }

        TestSession.DefaultConfiguration.invocation(TestConfig.Invocation.Concurrent).executeWrapped(testSuite) {
            assertEquals(TestConfig.Invocation.Concurrent, TestConfig.Invocation.current())
            assertNull(TestScopeContext.current())
        }

        assertFailsWith<IllegalArgumentException> {
            TestConfig.invocation(
                TestConfig.Invocation.Concurrent
            ).testScope(isEnabled = true).executeWrapped(testSuite) {
            }
        }.also {
            assertContains(it.message!!, "attempt was made to enable a 'TestScope' in combination with concurrent")
        }

        val testConfig = TestConfig.invocation(TestConfig.Invocation.Concurrent)
            .addPermits(TestConfig.Permit.TestScopeWithConcurrentInvocation)
            .testScope(isEnabled = true)
        testConfig.parameterize(testSuite) // This is required to pick up permits
        testConfig.executeWrapped(testSuite) {
            assertEquals(TestConfig.Invocation.Concurrent, TestConfig.Invocation.current())
            assertNotNull(TestScopeContext.current())
        }
    }

    @Test
    fun permits() {
        testPermits(testConfig = TestConfig)
        testPermits(
            testConfig = TestConfig.permits(TestConfig.Permit.SuiteWithoutChildren),
            TestConfig.Permit.SuiteWithoutChildren
        )
        testPermits(
            testConfig = TestConfig
                .permits(TestConfig.Permit.SuiteWithoutChildren, TestConfig.Permit.WrapperWithoutInnerInvocation),
            TestConfig.Permit.SuiteWithoutChildren,
            TestConfig.Permit.WrapperWithoutInnerInvocation
        )
    }

    @Test
    fun permitsReplaced() {
        testPermits(
            testConfig = TestConfig
                .permits(TestConfig.Permit.SuiteWithoutChildren)
                .permits(TestConfig.Permit.SuiteWithoutChildren, TestConfig.Permit.WrapperWithoutInnerInvocation),
            TestConfig.Permit.SuiteWithoutChildren,
            TestConfig.Permit.WrapperWithoutInnerInvocation
        )
    }

    @Test
    fun permitsAdded() {
        testPermits(
            testConfig = TestConfig
                .permits(TestConfig.Permit.SuiteWithoutChildren)
                .addPermits(TestConfig.Permit.WrapperWithoutInnerInvocation),
            TestConfig.Permit.SuiteWithoutChildren,
            TestConfig.Permit.WrapperWithoutInnerInvocation
        )
    }

    @Test
    fun permitsRemoved() {
        testPermits(
            testConfig = TestConfig
                .permits(TestConfig.Permit.SuiteWithoutChildren, TestConfig.Permit.WrapperWithoutInnerInvocation)
                .removePermits(TestConfig.Permit.WrapperWithoutInnerInvocation),
            TestConfig.Permit.SuiteWithoutChildren
        )
    }

    private fun testPermits(testConfig: TestConfig, vararg expectedPermits: TestConfig.Permit) = withTestFramework {
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
            assertEquals(TestConfig.Invocation.Sequential, TestConfig.Invocation.current())
            TestConfig.invocation(TestConfig.Invocation.Concurrent).executeWrapped(testSuite) {
                assertEquals(coroutineNameElement, currentCoroutineContext()[CoroutineName.Key])
                assertEquals(TestConfig.Invocation.Concurrent, TestConfig.Invocation.current())
            }
        }
    }

    @Test
    fun chaining() = withTestFramework {
        val testSuite by testSuite("testSuite") {}

        val coroutineNameElement = CoroutineName("TEST-CC")
        assertNull(currentCoroutineContext()[CoroutineName.Key])
        assertEquals(TestConfig.Invocation.Sequential, TestConfig.Invocation.current())
        TestConfig
            .coroutineContext(coroutineNameElement)
            .invocation(TestConfig.Invocation.Concurrent).executeWrapped(testSuite) {
                assertEquals(coroutineNameElement, currentCoroutineContext()[CoroutineName.Key])
                assertEquals(TestConfig.Invocation.Concurrent, TestConfig.Invocation.current())
            }
    }
}
