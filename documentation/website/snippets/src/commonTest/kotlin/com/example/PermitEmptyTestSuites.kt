package com.example

import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestSession
import de.infix.testBalloon.framework.core.addPermits

class ModuleSession :
    TestSession(
        testConfig = DefaultConfiguration.addPermits(
            TestConfig.Permit.SuiteWithoutChildren
        )
    )
