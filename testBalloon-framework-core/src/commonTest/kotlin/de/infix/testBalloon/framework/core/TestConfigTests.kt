package de.infix.testBalloon.framework.core

import de.infix.testBalloon.framework.core.internal.FrameworkTestUtilities
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
    fun coroutineContext() = FrameworkTestUtilities.withTestFramework {
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
    fun invocation() = FrameworkTestUtilities.withTestFramework {
        val testSuite by testSuite("testSuite") {}

        TestConfig.executeWrapped(testSuite) {
            assertEquals(TestConfig.Invocation.Sequential, TestConfig.Invocation.current())
        }

        TestConfig.invocation(TestConfig.Invocation.Concurrent).executeWrapped(testSuite) {
            assertEquals(TestConfig.Invocation.Concurrent, TestConfig.Invocation.current())
        }
    }

    @Test
    fun concurrencyAndTestScope() = FrameworkTestUtilities.withTestFramework {
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
    fun permitsSetting() {
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
    fun permitsAddition() {
        testPermits(
            testConfig = TestConfig
                .permits(TestConfig.Permit.SuiteWithoutChildren)
                .addPermits(TestConfig.Permit.WrapperWithoutInnerInvocation),
            TestConfig.Permit.SuiteWithoutChildren,
            TestConfig.Permit.WrapperWithoutInnerInvocation
        )
    }

    @Test
    fun permitsReplacement() {
        testPermits(
            testConfig = TestConfig
                .permits(TestConfig.Permit.SuiteWithoutChildren)
                .permits(TestConfig.Permit.SuiteWithoutChildren, TestConfig.Permit.WrapperWithoutInnerInvocation),
            TestConfig.Permit.SuiteWithoutChildren,
            TestConfig.Permit.WrapperWithoutInnerInvocation
        )
    }

    @Test
    fun permitsRemoval() {
        testPermits(
            testConfig = TestConfig
                .permits(TestConfig.Permit.SuiteWithoutChildren, TestConfig.Permit.WrapperWithoutInnerInvocation)
                .removePermits(TestConfig.Permit.WrapperWithoutInnerInvocation),
            TestConfig.Permit.SuiteWithoutChildren
        )
    }

    private fun testPermits(testConfig: TestConfig, vararg expectedPermits: TestConfig.Permit) =
        FrameworkTestUtilities.withTestFramework {
            val testSuite by testSuite("testSuite") {}

            testConfig.parameterize(testSuite)

            TestConfig.executeWrapped(testSuite) {
                assertContentEquals(parameters.permits.toList(), expectedPermits.toSet().toList())
            }
        }

    private class ParameterA : TestElement.KeyedParameter(Key) {
        companion object {
            val Key = object : Key<ParameterA> {}
        }
    }

    private class ParameterB : TestElement.KeyedParameter(Key) {
        companion object {
            val Key = object : Key<ParameterB> {}
        }
    }

    @Test
    fun parameterAddition() {
        testKeyedParameters(testConfig = TestConfig)
        val parameterA = ParameterA()
        testKeyedParameters(
            testConfig = TestConfig.parameter(ParameterA.Key) { parameterA },
            parameterA
        )
        val parameterB = ParameterB()
        testKeyedParameters(
            testConfig = TestConfig
                .parameter(ParameterA.Key) { parameterA }
                .parameter(ParameterB.Key) { parameterB },
            parameterA,
            parameterB
        )
    }

    @Test
    fun parameterReplacement() {
        val parameterA1 = ParameterA()
        val parameterA2 = ParameterA()
        testKeyedParameters(
            testConfig = TestConfig
                .parameter(ParameterA.Key) { parameterA1 }
                .parameter(ParameterA.Key) { parameterA2 },
            parameterA2
        )
    }

    @Test
    fun parameterRemoval() {
        val parameterA = ParameterA()
        testKeyedParameters(
            testConfig = TestConfig
                .parameter(ParameterA.Key) { parameterA }
                .parameter(ParameterA.Key) { null }
        )
        val parameterB = ParameterB()
        testKeyedParameters(
            testConfig = TestConfig
                .parameter(ParameterA.Key) { parameterA }
                .parameter(ParameterB.Key) { parameterB }
                .parameter(ParameterA.Key) { null },
            parameterB
        )
    }

    private fun testKeyedParameters(testConfig: TestConfig, vararg expectedParameters: TestElement.KeyedParameter) =
        FrameworkTestUtilities.withTestFramework {
            val testSuite by testSuite("testSuite") {}

            testConfig.parameterize(testSuite)

            TestConfig.executeWrapped(testSuite) {
                assertContentEquals(parameters.keyedParameters.values.toList(), expectedParameters.toSet().toList())
            }
        }

    @Test
    fun testScope() = FrameworkTestUtilities.withTestFramework {
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
    fun inheritance() = FrameworkTestUtilities.withTestFramework {
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
    fun chaining() = FrameworkTestUtilities.withTestFramework {
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
