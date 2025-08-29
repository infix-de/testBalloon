package blockingDetection

import de.infix.testBalloon.framework.TestBalloonExperimentalApi
import de.infix.testBalloon.framework.testPlatform
import de.infix.testBalloon.framework.testSuite

val IntegrationTest by testSuite {
    @OptIn(TestBalloonExperimentalApi::class)
    test("No blocking detection on ${testPlatform.displayName}") {
        // Non-JVM targets do not have blocking detection.
    }
}
