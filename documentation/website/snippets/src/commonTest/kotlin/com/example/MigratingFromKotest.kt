@file:Suppress("ktlint:standard:function-literal")
@file:OptIn(ExperimentalUuidApi::class)

package com.example

import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.aroundEachTest
import de.infix.testBalloon.framework.core.testSuite
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

// --8<-- [start:isolated-tests]
val IsolatedTests by testSuite {
    testFixture {
        Uuid.random() // (1)!
    } asParameterForEach { // (2)!
        test("one") { uuid ->
            println(uuid) // (3)!
        }
        test("two") { uuid ->
            println(uuid) // (4)!
        }
    }
}
// --8<-- [end:isolated-tests]

// --8<-- [start:lifecycle-hooks]
fun TestConfig.printStart() = aroundEachTest { testAction ->
    println("Starting a test $this")
    testAction() // (1)!
}

val LifecycleHookTests by testSuite(testConfig = TestConfig.printStart()) {
    test("this test should be alive") {
        println("Johnny5 is alive!")
    }
}
// --8<-- [end:lifecycle-hooks]
