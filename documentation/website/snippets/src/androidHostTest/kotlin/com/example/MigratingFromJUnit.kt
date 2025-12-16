@file:Suppress("ktlint:standard:function-literal")

package com.example

import de.infix.testBalloon.framework.core.testSuite
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.AfterClass
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

// --8<-- [start:junit-basics]
class JUnitBasics {
    @Test
    fun expected_to_pass() {
        Assert.assertEquals(4, 2 + 2)
    }

    @Test
    fun expected_to_fail() {
        Assert.assertEquals(5, 2 + 2)
    }
}
// --8<-- [end:junit-basics]

// --8<-- [start:testballoon-basics]
val FromJUnitBasics by testSuite {
    test("expected to pass") {
        Assert.assertEquals(4, 2 + 2)
    }

    test("expected to fail") {
        Assert.assertEquals(5, 2 + 2)
    }
}
// --8<-- [end:testballoon-basics]

// --8<-- [start:junit-test-level-fixture]
class JUnitTestLevelFixture {
    private lateinit var service: WeatherService

    @Before // (1)!
    fun setup() = runTest {
        service = FakeWeatherService()
        service.connect(token = "TestToken")
    }

    @After // (2)!
    fun teardown() = runTest {
        service.disconnect()
    }

    @Test
    fun `Temperature in Hamburg is 21_5 °C`() = runTest {
        Assert.assertEquals(21.5, service.location("Hamburg").temperature)
    }

    // more tests...
}
// --8<-- [end:junit-test-level-fixture]

// --8<-- [start:testballoon-test-level-fixture]
val FromJUnitTestLevelFixture by testSuite {
    testFixture {
        FakeWeatherService().apply {
            connect(token = "TestToken") // (1)!
        }
    } closeWith {
        disconnect() // (2)!
    } asParameterForEach { // (3)!

        test("Temperature in Hamburg is 21.5 °C") { service ->
            Assert.assertEquals(21.5, service.location("Hamburg").temperature)
        }

        // more tests...
    }
}
// --8<-- [end:testballoon-test-level-fixture]

// --8<-- [start:junit-suite-level-fixture]
class JUnitClassLevelFixture {
    companion object {
        private lateinit var service: WeatherService

        @JvmStatic
        @BeforeClass // (1)!
        fun setup(): Unit = runTest {
            service = FakeWeatherService()
            service.connect(token = "TestToken")
        }

        @JvmStatic
        @AfterClass // (2)!
        fun teardown() = runTest {
            service.disconnect()
        }
    }

    @Test
    fun `Temperature in Hamburg is 21_5 °C`() = runTest {
        Assert.assertEquals(
            21.5,
            service.location("Hamburg").temperature
        )
    }

    // more tests...
}
// --8<-- [end:junit-suite-level-fixture]

// --8<-- [start:testballoon-suite-level-fixture]
val FromJUnitClassLevelFixture by testSuite {
    testFixture {
        FakeWeatherService().apply {
            connect(token = "TestToken")
        }
    } closeWith {
        disconnect()
    } asParameterForAll { // (1)!

        test("Temperature in Hamburg is 21.5 °C") { service ->
            Assert.assertEquals(21.5, service.location("Hamburg").temperature)
        }

        // more tests...
    }
}
// --8<-- [end:testballoon-suite-level-fixture]

// --8<-- [start:junit-mixed-fixture]
class JUnitMixedFixture {
    companion object {
        private lateinit var service: WeatherService

        @JvmStatic
        @BeforeClass // (1)!
        fun setup(): Unit = runTest {
            service = FakeWeatherService()
        }
    }

    @Before // (2)!
    fun setup() = runTest {
        service.connect(token = "TestToken")
    }

    @After // (3)!
    fun teardown() = runTest {
        service.disconnect()
    }

    @Test
    fun `Temperature in Hamburg is 21_5 °C`() = runTest {
        Assert.assertEquals(21.5, service.location("Hamburg").temperature)
    }

    // more tests...
}
// --8<-- [end:junit-mixed-fixture]

// --8<-- [start:testballoon-mixed-fixture]
val FromJUnitMixedFixture by testSuite {
    val sharedService = testFixture { FakeWeatherService() }

    testFixture {
        sharedService().apply { // (1)!
            connect(token = "TestToken")
        }
    } closeWith {
        disconnect()
    } asParameterForEach {

        test("Temperature in Hamburg is 21.5 °C") { service ->
            Assert.assertEquals(21.5, service.location("Hamburg").temperature)
        }

        // more tests...
    }
}
// --8<-- [end:testballoon-mixed-fixture]

// --8<-- [start:junit-parameterized]
@RunWith(Parameterized::class)
class JUnit4Parameterized(val city: String, val expectedTemperature: Double) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0} {1}")
        fun data() =
            mapOf("Hamburg" to 21.5, "Munich" to 25.0, "Berlin" to 23.5)
                .map { (city, expectedTemperature) ->
                    arrayOf<Any>(city, expectedTemperature)
                }
    }

    private lateinit var service: WeatherService

    @Before
    fun setup() = runTest {
        service = FakeWeatherService()
        service.connect(token = "TestToken")
    }

    @After
    fun teardown() = runTest {
        service.disconnect()
    }

    @Test
    fun `Location has expected temperature`() = runTest {
        Assert.assertEquals(
            expectedTemperature,
            service.location(city).temperature
        )
    }
}
// --8<-- [end:junit-parameterized]

// --8<-- [start:testballoon-parameterized]
val FromJUnit4Parameterized by testSuite {
    testFixture {
        FakeWeatherService().apply {
            connect(token = "TestToken")
        }
    } closeWith {
        disconnect()
    } asParameterForEach {
        mapOf(
            "Hamburg" to 21.5,
            "Munich" to 25.0,
            "Berlin" to 23.5
        ).forEach { (city, expectedTemperature) ->
            test(
                "Temperature in $city is $expectedTemperature °C"
            ) { service ->
                Assert.assertEquals(
                    expectedTemperature,
                    service.location(city).temperature
                )
            }
        }
    }
}
// --8<-- [end:testballoon-parameterized]

interface WeatherService {
    suspend fun connect(token: String) {}
    suspend fun disconnect() {}
    suspend fun location(name: String): Location
}

interface Location {
    val name: String
    var temperature: Double
}

class FakeWeatherService : WeatherService {
    val locations = mapOf(
        "Hamburg" to 21.5,
        "Berlin" to 23.5,
        "Munich" to 25.0
    ).map { (city, temperature) ->
        city to FakeLocation(city, temperature)
    }.toMap()

    override suspend fun location(name: String) =
        locations[name] ?: throw Error("Could not find location '$name'")
}

class FakeLocation(
    override val name: String,
    override var temperature: Double
) : Location
