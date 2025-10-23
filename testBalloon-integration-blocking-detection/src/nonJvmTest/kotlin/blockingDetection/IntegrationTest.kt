package blockingDetection

import de.infix.testBalloon.framework.core.TestBalloonExperimentalApi
import de.infix.testBalloon.framework.core.testPlatform
import de.infix.testBalloon.framework.core.testSuite

val IntegrationTest by testSuite {
    @OptIn(TestBalloonExperimentalApi::class)
    test("No blocking detection on ${testPlatform.displayName}") {
        // Non-JVM targets do not have blocking detection.
    }
}
