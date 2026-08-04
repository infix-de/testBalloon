@file:Suppress("ClassName")

package de.infix.testBalloon.compilerPlugin.layer.kotlin2320

import de.infix.testBalloon.compilerPlugin.base.CompilerAdapter
import de.infix.testBalloon.compilerPlugin.base.DeclarationFinderBridge
import de.infix.testBalloon.compilerPlugin.base.IrGenerationExtensionBase
import de.infix.testBalloon.compilerPlugin.base.KotlinVersion
import de.infix.testBalloon.compilerPlugin.base.ModuleTransformer
import de.infix.testBalloon.compilerPlugin.base.asKotlinVersion
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar.ExtensionStorage
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.ir.builders.irAnnotation
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrMutableAnnotationContainer
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import kotlin.collections.forEachIndexed

@OptIn(UnsafeDuringIrConstructionAPI::class)
class CompilerAdapter_2_3_20(configuration: Configuration) : CompilerAdapter(configuration) {
    override val adapterVersion: KotlinVersion = "2.3.20".asKotlinVersion()

    @OptIn(ExperimentalCompilerApi::class)
    override fun ExtensionStorage.registerExtensions(compilerConfiguration: CompilerConfiguration) {
        FirExtensionRegistrarAdapter.registerExtension(
            CompilerPluginFirExtensionRegistrar { session ->
                FirDeclarationGenerationExtensionBase(session)
            }
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

class ModuleTransformer_2_3_20(configuration: Configuration) : ModuleTransformer(configuration) {
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun IrMutableAnnotationContainer.addAnnotation(
        irTargetSymbol: IrSymbol,
        irAnnotationClassSymbol: IrClassSymbol,
        vararg annotationArguments: IrExpression
    ) {
        with(irTargetSymbol.owner) {
            annotations += irTargetSymbol.irAnnotation(irConstructorCall(irAnnotationClassSymbol).symbol).apply {
                annotationArguments.forEachIndexed { index, annotationArgument ->
                    arguments[index] = annotationArgument
                }
            }
        }
    }

    private fun IrSymbol.irAnnotation(irClassSymbol: IrConstructorSymbol) =
        pluginContext.irBuiltIns.createIrBuilder(this).irAnnotation(irClassSymbol, emptyList())

    override val defaultPropertyAccessor: IrDeclarationOrigin
        get() = IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
}

class DeclarationFinderAdapter_2_3_20(pluginContext: IrPluginContext) : DeclarationFinderBridge {
    private val declarationFinder = pluginContext.finderForBuiltins()

    override fun findClass(classId: ClassId): IrClassSymbol? = declarationFinder.findClass(classId)

    override fun findFunctions(callableId: CallableId): Collection<IrSimpleFunctionSymbol> =
        declarationFinder.findFunctions(callableId)

    override fun findProperties(callableId: CallableId): Collection<IrPropertySymbol> =
        declarationFinder.findProperties(callableId)
}
