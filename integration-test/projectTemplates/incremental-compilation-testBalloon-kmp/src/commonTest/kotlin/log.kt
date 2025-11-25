import de.infix.testBalloon.framework.core.TestExecutionScope
import de.infix.testBalloon.framework.core.testPlatform

fun TestExecutionScope.log(message: String) {
    println("##LOG(${testPlatform.displayName} – $testElementPath: $message)LOG##")
}
