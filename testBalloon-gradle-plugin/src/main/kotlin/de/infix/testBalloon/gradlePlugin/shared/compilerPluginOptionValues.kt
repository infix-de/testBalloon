package de.infix.testBalloon.gradlePlugin.shared

import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalApi

/**
 * Returns the compiler plugin options and their values, as defined by the plugin's property and extension settings.
 */
@OptIn(TestBalloonInternalApi::class)
internal fun compilerPluginOptionValues(
    testBalloonExtension: TestBalloonGradleExtension,
    testBalloonProperties: TestBalloonGradleProperties
): Map<String, String> = mapOf(
    "debugLevel" to testBalloonExtension.debugLevel.toString(),
    "junit4AutoIntegrationEnabled" to (
        testBalloonExtension.junit4AutoIntegrationEnabled ?: testBalloonProperties.junit4AutoIntegrationEnabled ?: true
        ).toString(),
    "testModuleRegex" to testBalloonProperties.testModuleRegex
)
