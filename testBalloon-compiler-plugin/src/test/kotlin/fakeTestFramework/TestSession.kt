package fakeTestFramework

import de.infix.testBalloon.framework.AbstractTestSession
import de.infix.testBalloon.framework.TestDiscoverable

@TestDiscoverable
open class TestSession :
    TestSuite(name = "TestSession"),
    AbstractTestSession
