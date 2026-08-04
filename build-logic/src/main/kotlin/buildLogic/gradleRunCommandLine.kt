package buildLogic

import org.apache.tools.ant.taskdefs.condition.Os

fun gradleRunCommandLine(vararg arguments: String) = if (Os.isFamily(Os.FAMILY_WINDOWS)) {
    listOf("cmd", "/c", "gradlew.bat", *arguments)
} else {
    listOf("./gradlew", *arguments)
}
