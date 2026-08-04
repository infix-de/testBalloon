@file:Suppress("ClassName")

package de.infix.testBalloon.compilerPlugin.layer.kotlin230

import de.infix.testBalloon.compilerPlugin.base.CompilerAdapter
import de.infix.testBalloon.compilerPlugin.base.DeclarationFinderBridge
import de.infix.testBalloon.compilerPlugin.base.IrGenerationExtensionBase
import de.infix.testBalloon.compilerPlugin.base.KotlinVersion
import de.infix.testBalloon.compilerPlugin.base.ModuleTransformer
import de.infix.testBalloon.compilerPlugin.base.asKotlinVersion
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar.ExtensionStorage
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrMutableAnnotationContainer
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId

class CompilerAdapter_2_3_0(configuration: Configuration) : CompilerAdapter(configuration) {
    override val adapterVersion: KotlinVersion = "2.3.0".asKotlinVersion()

    @OptIn(ExperimentalCompilerApi::class)
    override fun ExtensionStorage.registerExtensions(compilerConfiguration: CompilerConfiguration) {
        IrGenerationExtension.registerExtension(
            IrGenerationExtensionBase(
                compilerConfiguration = compilerConfiguration,
                declarationFinder = { pluginContext -> DeclarationFinderAdapter_2_3_0(pluginContext) },
                moduleTransformer = { ModuleTransformer_2_3_0(it) }
            )
        )
    }
}

class ModuleTransformer_2_3_0(configuration: Configuration) : ModuleTransformer(configuration) {
    override fun IrMutableAnnotationContainer.addAnnotation(
        irTargetSymbol: IrSymbol,
        irAnnotationClassSymbol: IrClassSymbol,
        vararg annotationArguments: IrExpression
    ) {
        @OptIn(UnsafeDuringIrConstructionAPI::class)
        with(irTargetSymbol.owner) {
            annotations += irConstructorCall(irAnnotationClassSymbol, *annotationArguments)
        }
    }

    override val defaultPropertyAccessor: IrDeclarationOrigin
        get() = IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
}

class DeclarationFinderAdapter_2_3_0(private val pluginContext: IrPluginContext) : DeclarationFinderBridge {
    override fun findClass(classId: ClassId): IrClassSymbol? = pluginContext.referenceClass(classId)

    override fun findFunctions(callableId: CallableId): Collection<IrSimpleFunctionSymbol> =
        pluginContext.referenceFunctions(callableId)

    override fun findProperties(callableId: CallableId): Collection<IrPropertySymbol> =
        pluginContext.referenceProperties(callableId)
}
