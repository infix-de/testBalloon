@file:OptIn(ExperimentalUuidApi::class)

package com.example

import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.aroundEachTest
import de.infix.testBalloon.framework.core.testSuite
import de.infix.testBalloon.framework.shared.TestRegistering
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

// --8<-- [start:isolated-tests]
val IsolatedTests by testSuite {
    class Context {
        val id = Uuid.random()
    }

    @TestRegistering // (4)!
    fun test(name: String, action: suspend Context.() -> Unit) =
        this.test(name) { Context().action() } // (1)!

    test("one") {
        println(id) // (2)!
    }
    test("two") {
        println(id) // (3)!
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
