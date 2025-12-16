@file:Suppress("ktlint:standard:function-literal")

package com.com.example

import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.aroundEachTest
import de.infix.testBalloon.framework.core.testSuite
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.InvocationInterceptor
import org.junit.jupiter.api.extension.ReflectiveInvocationContext
import java.lang.reflect.Method
import kotlin.test.Test
import kotlin.time.measureTime

// --8<-- [start:junit6-extension]
@ExtendWith(TimingExtension::class)
class JUnit6WithExtension {
    @Test
    fun `some test`() {
        // Code to be timed
    }
}

class TimingExtension : InvocationInterceptor {
    override fun interceptTestMethod(
        invocation: InvocationInterceptor.Invocation<Void?>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext
    ) {
        val duration = measureTime {
            super.interceptTestMethod(
                invocation,
                invocationContext,
                extensionContext
            )
        }
        println(
            "TIME: ${extensionContext.requiredTestMethod.name} took $duration."
        )
    }
}
// --8<-- [end:junit6-extension]

// --8<-- [start:testballoon-from-junit6-extension]
val FromJUnit6WithExtension by testSuite(testConfig = TestConfig.timed()) {
    test("some test") {
        // Code to be timed
    }
}

fun TestConfig.timed() = aroundEachTest { action ->
    val duration = measureTime {
        action()
    }
    println("TIME: $testElementPath took $duration.")
}
// --8<-- [end:testballoon-from-junit6-extension]
