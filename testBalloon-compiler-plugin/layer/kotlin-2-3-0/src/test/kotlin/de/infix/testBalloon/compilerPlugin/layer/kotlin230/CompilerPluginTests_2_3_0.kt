@file:OptIn(TestBalloonInternalApi::class)

package de.infix.testBalloon.compilerPlugin.layer.kotlin230

import CompilerPluginBaseTests
import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalApi
import kotlin.test.Test

@Suppress("ClassName")
private class CompilerPluginTests_2_3_0 {
    private val base = CompilerPluginBaseTests(addExternalEntryPointSourceFile = true)

    @Test
    fun versioning() = base.versioning(adapterVersion = "2.3.0", compilerVersion = "2.3.0")

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
