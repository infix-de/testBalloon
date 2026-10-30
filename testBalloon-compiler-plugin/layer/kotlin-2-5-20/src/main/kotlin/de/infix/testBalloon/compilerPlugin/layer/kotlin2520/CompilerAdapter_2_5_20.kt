@file:Suppress("ktlint:standard:class-naming", "ClassName")

package de.infix.testBalloon.compilerPlugin.layer.kotlin2520

import de.infix.testBalloon.compilerPlugin.layer.base.CompilerAdapter
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.fileParent
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar.ExtensionStorage
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.fileMappingTracker
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.ir.builders.irAnnotation
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrMutableAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.util.addFakeOverrides
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import java.io.File

@Suppress("ClassName", "unused")
class CompilerAdapter_2_5_20(configuration: Configuration) : CompilerAdapter(configuration) {

    @OptIn(ExperimentalCompilerApi::class)
    override fun ExtensionStorage.registerExtensions(compilerConfiguration: CompilerConfiguration) {
        FirExtensionRegistrarAdapter.registerExtension(
            CompilerPluginFirExtensionRegistrar { session -> FirDeclarationGenerationExtensionBase(session) }
        )
        IrGenerationExtension.registerExtension(
            IrGenerationExtensionBase(
                compilerConfiguration = compilerConfiguration,
                declarationFinder = { pluginContext -> DeclarationFinderAdapter_2_5_20(pluginContext) },
                moduleTransformer = { ModuleTransformer_2_5_20(it) }
            )
        )
    }
}

class ModuleTransformer_2_5_20(configuration: Configuration) : ModuleTransformer(configuration) {
    private val fileMappingTracker = configuration.compilerConfiguration.fileMappingTracker

    override fun IrClass.addFakeOverridesCompatibly(typeSystem: IrTypeSystemContext) {
        addFakeOverrides(typeSystem)
    }

    override fun registerReference(entryPointFile: IrFile, referencedDeclaration: IrDeclarationWithName) {
        pluginContext.recordLookup(referencedDeclaration, entryPointFile)
        fileMappingTracker?.recordSourceReferencedByCompilerPlugin(
            File(referencedDeclaration.fileParent.path)
        )
    }

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

class DeclarationFinderAdapter_2_5_20(pluginContext: IrPluginContext) : DeclarationFinderAdapter {
    private val declarationFinder = pluginContext.finderForBuiltins()

    override fun findClass(classId: ClassId): IrClassSymbol? = declarationFinder.findClass(classId)

    override fun findFunctions(callableId: CallableId): Collection<IrSimpleFunctionSymbol> =
        declarationFinder.findFunctions(callableId)

    override fun findProperties(callableId: CallableId): Collection<IrPropertySymbol> =
        declarationFinder.findProperties(callableId)
}
