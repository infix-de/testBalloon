package com.example

import de.infix.testBalloon.framework.core.TestBalloonExperimentalApi
import de.infix.testBalloon.framework.core.TestPermit
import de.infix.testBalloon.framework.core.TestSession
import de.infix.testBalloon.framework.core.addPermits

@OptIn(TestBalloonExperimentalApi::class)
class ModuleSession :
    TestSession(
        testConfig = DefaultConfiguration.addPermits(
            TestPermit.SUITE_WITHOUT_CHILDREN
        )
    )
