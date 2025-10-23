@file:Suppress("PackageDirectoryMismatch", "unused")
@file:OptIn(TestBalloonInternalApi::class)

// The compiler plugin requires this package name.

package de.infix.testBalloon.framework.core.internal

import de.infix.testBalloon.framework.shared.AbstractTestSession
import de.infix.testBalloon.framework.shared.AbstractTestSuite
import de.infix.testBalloon.framework.shared.internal.InvokedByGeneratedCode
import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalApi

@InvokedByGeneratedCode
internal fun initializeTestFramework(testSession: AbstractTestSession?, arguments: Array<String>? = null) {}

@Suppress("RedundantSuspendModifier")
@InvokedByGeneratedCode
internal suspend fun configureAndExecuteTests(suites: Array<AbstractTestSuite>) {}
