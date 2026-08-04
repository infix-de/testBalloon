package de.infix.testBalloon.compilerPlugin.layer.kotlin240

import de.infix.testBalloon.compilerPlugin.base.CompilerAdapter
import de.infix.testBalloon.compilerPlugin.base.IrGenerationExtensionBase
import de.infix.testBalloon.compilerPlugin.base.KotlinVersion
import de.infix.testBalloon.compilerPlugin.base.asKotlinVersion
import de.infix.testBalloon.compilerPlugin.layer.kotlin2320.CompilerPluginFirExtensionRegistrar
import de.infix.testBalloon.compilerPlugin.layer.kotlin2320.DeclarationFinderAdapter_2_3_20
import de.infix.testBalloon.compilerPlugin.layer.kotlin2320.FirDeclarationGenerationExtensionBase
import de.infix.testBalloon.compilerPlugin.layer.kotlin2320.ModuleTransformer_2_3_20
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar.ExtensionStorage
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI

@Suppress("ClassName")
@OptIn(UnsafeDuringIrConstructionAPI::class)
class CompilerAdapter_2_4_0(configuration: Configuration) : CompilerAdapter(configuration) {
    override val adapterVersion: KotlinVersion = "2.4.0".asKotlinVersion()

    @OptIn(ExperimentalCompilerApi::class)
    override fun ExtensionStorage.registerExtensions(compilerConfiguration: CompilerConfiguration) {
        FirExtensionRegistrarAdapter.registerExtension(
            CompilerPluginFirExtensionRegistrar { session -> FirDeclarationGenerationExtensionBase(session) }
        )
        IrGenerationExtension.registerExtension(
            IrGenerationExtensionBase(
                compilerConfiguration = compilerConfiguration,
                declarationFinder = { pluginContext -> DeclarationFinderAdapter_2_3_20(pluginContext) },
                moduleTransformer = { ModuleTransformer_2_3_20(it) }
            )
        )
    }
}
