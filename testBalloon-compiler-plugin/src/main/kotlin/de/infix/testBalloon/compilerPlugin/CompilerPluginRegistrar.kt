package de.infix.testBalloon.compilerPlugin

import de.infix.testBalloon.framework.shared.internal.DebugLevel
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration

@OptIn(ExperimentalCompilerApi::class)
class CompilerPluginRegistrar : CompilerPluginRegistrar() {
    override val supportsK2 = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val disablingReason = Options.disablingReason.value(configuration)

        if (disablingReason.isEmpty()) {
            IrGenerationExtension.registerExtension(CompilerPluginIrGenerationExtension(configuration))
        } else {
            val messageCollector = configuration.get(
                CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY,
                MessageCollector.NONE
            )
            val debugLevel = Options.debugLevel.value(configuration)

            if (debugLevel > DebugLevel.NONE) {
                messageCollector.report(
                    CompilerMessageSeverity.STRONG_WARNING,
                    "$PLUGIN_DISPLAY_NAME: [DEBUG] compiler plugin is disabled ($disablingReason)."
                )
            }
        }
    }
}
