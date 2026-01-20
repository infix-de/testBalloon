package de.infix.testBalloon.compilerPlugin

import buildConfig.BuildConfig.PROJECT_COMPILER_PLUGIN_ID
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

@OptIn(ExperimentalCompilerApi::class)
class CompilerPluginRegistrar : CompilerPluginRegistrar() {
    override val pluginId: String = PROJECT_COMPILER_PLUGIN_ID
    override val supportsK2 = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        IrGenerationExtension.registerExtension(CompilerPluginIrGenerationExtension(configuration))
    }
}
