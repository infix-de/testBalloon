import de.infix.testBalloon.framework.core.Test
import de.infix.testBalloon.framework.core.testPlatform

fun Test.ExecutionScope.log(message: String) {
    println("##LOG(${testPlatform.displayName} – $testElementPath: $message)LOG##")
}
