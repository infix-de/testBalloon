import de.infix.testBalloon.framework.TestExecutionScope
import de.infix.testBalloon.framework.testPlatform

fun TestExecutionScope.log(message: String) {
    println("##LOG(${testPlatform.displayName} – $testElementPath: $message)LOG##")
}
