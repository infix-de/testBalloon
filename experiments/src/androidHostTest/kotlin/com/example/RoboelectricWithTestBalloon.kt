package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import de.infix.testBalloon.framework.core.testSuite
import de.infix.testBalloon.integration.roboelectric.RoboelectricTestSuite
import de.infix.testBalloon.integration.roboelectric.roboelectricTestSuite

val RoboelectricWithTestBalloon by testSuite {
    roboelectricTestSuite("first", MyRoboelectricTests::class)

    roboelectricTestSuite("second", MyRoboelectricTests::class)

    testSuite("other") {
        test("ot") {
            val app = getApplicationContext<Application>()
            println("$testElementPath: app=$app")
        }
    }
}

class MyRoboelectricTests :
    RoboelectricTestSuite({
        test("hello") {
            val app = getApplicationContext<Application>()
            println("$testElementPath: app=$app")
        }
    })
