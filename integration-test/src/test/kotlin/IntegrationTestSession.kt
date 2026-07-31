import de.infix.testBalloon.framework.core.TestCompartment
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestSession
import de.infix.testBalloon.framework.core.invocation
import de.infix.testBalloon.framework.core.testPlatform
import de.infix.testBalloon.framework.core.testScope
import kotlin.time.Duration.Companion.minutes

class IntegrationTestSession :
    TestSession(defaultCompartment = {
        if (testPlatform.environment("CI") != null) {
            CITestCompartment()
        } else {
            TestCompartment.Concurrent
        }
    })

private class CITestCompartment :
    TestCompartment(
        name = "CI",
        testConfig = TestConfig
            .invocation(TestConfig.Invocation.Sequential)
            .testScope(isEnabled = true, timeout = 24.minutes)
    )
