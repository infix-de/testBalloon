package blockingDetection

import de.infix.testBalloon.framework.core.testPlatform
import de.infix.testBalloon.framework.core.testSuite

val IntegrationTest by testSuite {
    test("No blocking detection on ${testPlatform.displayName}") {
        // Non-JVM targets do not have blocking detection.
    }
}
