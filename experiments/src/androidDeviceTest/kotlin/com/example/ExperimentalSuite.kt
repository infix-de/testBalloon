package com.example

import de.infix.testBalloon.framework.core.TestSuiteScope
import de.infix.testBalloon.framework.core.testSuite

val E1 by testSuite {
    series()
}

private val baseTestName = buildString { repeat(30) { append("1234567890") } }

private fun TestSuiteScope.series() {
    val baseTestName = buildString { repeat(30) { append("1234567890") } }
    for (length in 100..250) {
        test(baseTestName.take(length)) {
            println("Testing $length")
        }
    }
}

// val E2 by testSuite {
//     repeat(150) {
//         test(baseTestName.take(127)) {}
//     }
// }
