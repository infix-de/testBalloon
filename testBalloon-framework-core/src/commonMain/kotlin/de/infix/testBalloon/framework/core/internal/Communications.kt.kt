package de.infix.testBalloon.framework.core.internal

import de.infix.testBalloon.framework.core.testPlatform
import de.infix.testBalloon.framework.shared.internal.EnvironmentVariable
import de.infix.testBalloon.framework.shared.internal.ReportingMode
import de.infix.testBalloon.framework.shared.internal.ReportingMode.Amper

internal fun EnvironmentVariable.value(): String? = testPlatform.environment(name)

internal val ReportingMode.supportsNesting: Boolean get() = this == Amper || reportingSupportsNesting

private val reportingSupportsNesting by lazy {
    EnvironmentVariable.TESTBALLOON_REPORTING_FEATURES.value()?.contains("nesting") == true
}
