package de.infix.testBalloon.compilerPlugin.base

import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.comparables.shouldNotBeEqualComparingTo
import io.kotest.matchers.comparables.shouldNotBeLessThan
import kotlin.test.Test

class KotlinVersionTest {
    @Test
    fun regularOrder() {
        verifyOrder("1.0.0 < 1.0.2 < 1.0.11 < 1.2.3 < 1.13.3 < 2.0.0 < 12.0.0")
    }

    @Test
    fun prereleaseOrder() {
        verifyOrder(
            "1.0.0-Alpha < 1.0.0-Alpha1 < 1.0.0-Alpha2 < 1.0.0-Beta < 1.0.0-Beta2 < 1.0.0-Beta11 < 1.0.0-RC1 < 1.0.0"
        )
    }

    @Test
    fun alphaDevOrder() {
        verifyOrder(
            "1.0.0-Alpha < 1.0.0-dev-234 < 1.0.0"
        )
    }

    @Test
    fun betaDevOrder() {
        verifyOrder(
            "1.0.0-Beta2 < 1.0.0-dev-234 < 1.0.0"
        )
    }

    @Test
    fun rcDevOrder() {
        verifyOrder(
            "1.0.0-RC1 < 1.0.0-dev-234 < 1.0.0"
        )
    }

    private fun verifyOrder(spec: String) {
        spec.split(" < ").map { it.asKotlinVersion() }.zipWithNext().forEach { (left, right) ->
            left shouldBeLessThan right
            right shouldNotBeLessThan left
            left shouldNotBeEqualComparingTo right
        }
    }
}
