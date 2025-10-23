package fakeTestFramework

import de.infix.testBalloon.framework.shared.AbstractTestSession
import de.infix.testBalloon.framework.shared.TestDiscoverable

@TestDiscoverable
open class TestSession :
    TestSuite(name = "TestSession"),
    AbstractTestSession
