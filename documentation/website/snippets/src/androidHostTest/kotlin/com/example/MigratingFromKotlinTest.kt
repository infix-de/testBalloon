@file:Suppress("ktlint:standard:function-literal")

package com.example

import de.infix.testBalloon.framework.core.testSuite
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

// --8<-- [start:kotlinTest-basics]
class KotlinTestBasics {
    @Test
    fun expected_to_pass() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun expected_to_fail() {
        assertEquals(5, 2 + 2)
    }
}
// --8<-- [end:kotlinTest-basics]

// --8<-- [start:testballoon-basics]
val FromKotlinTestBasics by testSuite {
    test("expected to pass") {
        assertEquals(4, 2 + 2)
    }

    test("expected to fail") {
        assertEquals(5, 2 + 2)
    }
}
// --8<-- [end:testballoon-basics]

// --8<-- [start:kotlinTest-test-level-fixture]
class KotlinTestFixture {
    private lateinit var service: WeatherService

    @BeforeTest
    fun setup() = runTest {
        service = FakeWeatherService()
        service.connect(token = "TestToken")
    }

    @AfterTest
    fun teardown() = runTest {
        service.disconnect()
    }

    @Test
    fun `Temperature in Hamburg is 21_5 °C`() = runTest {
        assertEquals(21.5, service.location("Hamburg").temperature)
    }

    // more tests...
}
// --8<-- [end:kotlinTest-test-level-fixture]

// --8<-- [start:testballoon-test-level-fixture]
val FromKotlinTestFixture by testSuite {
    testFixture {
        FakeWeatherService().apply {
            connect(token = "TestToken") // (1)!
        }
    } closeWith {
        disconnect() // (2)!
    } asParameterForEach { // (3)!

        test("Temperature in Hamburg is 21.5 °C") { service ->
            assertEquals(21.5, service.location("Hamburg").temperature)
        }

        // more tests...
    }
}
// --8<-- [end:testballoon-test-level-fixture]
