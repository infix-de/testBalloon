package de.infix.testBalloon.compilerPlugin.layer.kotlin2410

import CompilerPluginBaseTests
import kotlin.test.Test

@Suppress("ClassName")
private class CompilerPluginTests_2_4_10 {
    private val base = CompilerPluginBaseTests()

    @Test
    fun versioning() = base.versioning(adapterVersion = "2.4.0", compilerVersion = "2.4.10")

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
