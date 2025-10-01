import de.infix.testBalloon.framework.TestBalloonExperimentalApi
import de.infix.testBalloon.framework.internal.TestBalloonInternalApi
import de.infix.testBalloon.framework.testSuite

@OptIn(TestBalloonInternalApi::class, TestBalloonExperimentalApi::class)
val DisplayNameTests by testSuite(displayName = "dn") {
    testSuite("suite 1", displayName = "s1") {
        test("test 1", displayName = "t1") {
            println("$testElementPath")
        }
        testSuite("suite 1.1", displayName = "s1.1") {
            test("test 1.1", displayName = "t1.1") {
                println("$testElementPath")
            }
        }
    }
}

val NoDisplayNames by testSuite {
    testSuite("suite 1") {
        test("test 1") {
            println("$testElementPath")
        }
        testSuite("suite 1.1") {
            test("test 1.1") {
                println("$testElementPath")
            }
        }
    }
}
