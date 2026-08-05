@file:OptIn(TestBalloonInternalApi::class)

package de.infix.testBalloon.compilerPlugin.layer.kotlin220

import CompilerPluginBaseTests
import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalApi
import kotlin.test.Test

@Suppress("ClassName")
private class CompilerPluginTests_2_2_0 {
    private val base = CompilerPluginBaseTests(addExternalEntryPointSourceFile = true)

    @Test
    fun versioning() = base.versioning(adapterVersion = "2.2.0", compilerVersion = "2.2.0")

    @Test
    fun initialization() = base.initialization()

    @Test
    fun insistOnSingleTestSession() = base.insistOnSingleTestSession()

    @Test
    fun topLevelSuiteVisibility() = base.topLevelSuiteVisibility()

    @Test
    fun topLevelSuiteWithArgumentReordering() = base.topLevelSuiteWithArgumentReordering()

    @Test
    fun discoveryDebugLogging() = base.discoveryDebugLogging()

    @Test
    fun defectiveFrameworkLibraryDependency() = base.defectiveFrameworkLibraryDependency()
}
