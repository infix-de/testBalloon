package fakeTestFramework

import de.infix.testBalloon.framework.shared.AbstractTestSession
import de.infix.testBalloon.framework.shared.TestRegistering

@TestRegistering
open class TestSession :
    TestSuite(name = "TestSession"),
    AbstractTestSession
