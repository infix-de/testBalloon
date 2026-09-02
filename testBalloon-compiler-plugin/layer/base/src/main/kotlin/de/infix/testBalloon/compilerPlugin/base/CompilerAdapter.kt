package de.infix.testBalloon.compilerPlugin.base

import de.infix.testBalloon.framework.shared.internal.KotlinVersion
import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalApi
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar.ExtensionStorage
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

@OptIn(TestBalloonInternalApi::class)
abstract class CompilerAdapter(val configuration: Configuration) {
    data class Configuration(
        val pluginId: String,
        val compilerVersion: KotlinVersion,
        val adapterVersion: KotlinVersion
    )

    @OptIn(ExperimentalCompilerApi::class)
    abstract fun ExtensionStorage.registerExtensions(compilerConfiguration: CompilerConfiguration)
}
