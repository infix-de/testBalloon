@file:Suppress("PackageDirectoryMismatch", "unused")
@file:OptIn(TestBalloonInternalApi::class)

// The compiler plugin requires this package name.

package de.infix.testBalloon.framework.internal

import de.infix.testBalloon.framework.AbstractTestSession
import de.infix.testBalloon.framework.AbstractTestSuite

@InvokedByGeneratedCode
internal fun initializeTestFramework(testSession: AbstractTestSession?, arguments: Array<String>? = null) {}

@Suppress("RedundantSuspendModifier")
@InvokedByGeneratedCode
internal suspend fun configureAndExecuteTests(suites: Array<AbstractTestSuite>) {}
