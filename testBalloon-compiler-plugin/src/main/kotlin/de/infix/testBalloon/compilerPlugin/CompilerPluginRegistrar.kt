package de.infix.testBalloon.compilerPlugin

import buildConfig.BuildConfig.PROJECT_COMPILER_PLUGIN_ID
import de.infix.testBalloon.framework.shared.internal.DebugLevel
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.fir.moduleData

@OptIn(ExperimentalCompilerApi::class)
class CompilerPluginRegistrar : CompilerPluginRegistrar() {
    override val pluginId: String = PROJECT_COMPILER_PLUGIN_ID
    override val supportsK2 = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val disablingReason = Options.disablingReason.value(configuration)

        if (disablingReason.isEmpty()) {
            FirExtensionRegistrarAdapter.registerExtension(CompilerPluginFirExtensionRegistrar(configuration))
            IrGenerationExtension.registerExtension(CompilerPluginIrGenerationExtension(configuration))
        } else {
            val messageCollector = configuration.get(
                CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY,
                MessageCollector.NONE
            )
            val debugLevel = Options.debugLevel.value(configuration)

            if (debugLevel > DebugLevel.NONE) {
                messageCollector.report(
                    CompilerMessageSeverity.WARNING,
                    "$PLUGIN_DISPLAY_NAME: [DEBUG] compiler plugin is disabled ($disablingReason)."
                )
            }
        }
    }
}

class CompilerPluginFirExtensionRegistrar(val configuration: CompilerConfiguration) : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +FirDeclarationGenerationExtension.Factory { session ->
            val isTargetTestModule = !session.moduleData.isCommon

            if (isTargetTestModule) {
                CompilerPluginFrontendExtension(session)
            } else {
                NoOpFirDeclarationGenerationExtension(session)
            }
        }
    }

    private class NoOpFirDeclarationGenerationExtension(session: FirSession) :
        FirDeclarationGenerationExtension(session)
}
