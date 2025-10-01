import de.infix.testBalloon.framework.TestBalloonExperimentalApi
import de.infix.testBalloon.framework.testPlatform
import de.infix.testBalloon.framework.testSuite

val EnvironmentTests by testSuite {
    test("env") {
        for (variableName in listOf("TESTBALLOON_INCLUDE", "TESTBALLOON_EXCLUDE", "TESTBALLOON_REPORTING")) {
            @OptIn(TestBalloonExperimentalApi::class)
            println("$variableName=${testPlatform.environment(variableName)}")
        }
    }
}
