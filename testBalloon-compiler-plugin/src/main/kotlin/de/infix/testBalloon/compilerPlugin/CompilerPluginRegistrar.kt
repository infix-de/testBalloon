package de.infix.testBalloon.compilerPlugin

import buildConfig.BuildConfig.PROJECT_COMPILER_PLUGIN_ID
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
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
        FirExtensionRegistrarAdapter.registerExtension(CompilerPluginFirExtensionRegistrar(configuration))
        IrGenerationExtension.registerExtension(CompilerPluginIrGenerationExtension(configuration))
    }
}

class CompilerPluginFirExtensionRegistrar(val configuration: CompilerConfiguration) : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        val testModuleRegex = Regex(Options.testModuleRegex.value(configuration))

        +FirDeclarationGenerationExtension.Factory { session ->
            val isTargetTestModule = !session.moduleData.isCommon &&
                testModuleRegex.containsMatchIn(session.moduleData.name.asStringStripSpecialMarkers())

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
