package de.infix.testBalloon.framework.core.internal

import de.infix.testBalloon.framework.core.TestElement
import kotlin.test.assertContentEquals

internal fun List<String>.assertContainsInOrder(expectedElements: List<String>, exhaustive: Boolean = false) {
    if (exhaustive && expectedElements.size != size) {
        throw AssertionError("Expected ${expectedElements.size} elements but got $size")
    }
    val firstExpectedElement = expectedElements.first()
    val actualElementsSlice = drop(indexOfFirst { it == firstExpectedElement }).take(expectedElements.size)
    assertContentEquals(expectedElements, actualElementsSlice)
}

internal fun Throwable.assertMessageStartsWith(phrase: String) {
    if (message?.startsWith(phrase) != true) {
        throw AssertionError("Exception message did not start with '$phrase', but is '$message'")
    }
}

fun List<TestElement.Event>.assertElementPathsContainInOrder(expectedPaths: List<String>, exhaustive: Boolean = false) {
    map { it.element.testElementPath.internalId }.assertContainsInOrder(expectedPaths, exhaustive)
}
