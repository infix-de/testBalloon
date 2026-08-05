package de.infix.testBalloon.compilerPlugin.base

import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar.ExtensionStorage
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

abstract class CompilerAdapter(val configuration: Configuration) {
    data class Configuration(val pluginId: String, val compilerVersion: KotlinVersion)

    abstract val adapterVersion: KotlinVersion

    @OptIn(ExperimentalCompilerApi::class)
    abstract fun ExtensionStorage.registerExtensions(compilerConfiguration: CompilerConfiguration)
}
