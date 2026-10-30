package de.infix.testBalloon.compilerPlugin.layer.kotlin2520

import CompilerPluginBaseTests
import kotlin.test.Test

@Suppress("ClassName")
private class CompilerPluginTests_2_5_20 {
    private val base = CompilerPluginBaseTests()

    @Test
    fun versioning() = base.versioning(adapterVersion = "2.4.0", compilerVersion = "2.5.20-dev-2623")

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
