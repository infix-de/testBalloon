@file:Suppress("ClassName")

package de.infix.testBalloon.compilerPlugin.layer.kotlin230

import de.infix.testBalloon.compilerPlugin.base.CompilerAdapter
import de.infix.testBalloon.compilerPlugin.base.IrGenerationExtensionBase
import de.infix.testBalloon.compilerPlugin.layer.kotlin220.DeclarationFinderAdapter_2_2_0
import de.infix.testBalloon.compilerPlugin.layer.kotlin220.ModuleTransformer_2_2_0
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.jvm.ir.fileParent
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar.ExtensionStorage
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.fileMappingTracker
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.util.addFakeOverrides
import java.io.File

@Suppress("unused")
class CompilerAdapter_2_3_0(configuration: Configuration) : CompilerAdapter(configuration) {

    @OptIn(ExperimentalCompilerApi::class)
    override fun ExtensionStorage.registerExtensions(compilerConfiguration: CompilerConfiguration) {
        IrGenerationExtension.registerExtension(
            IrGenerationExtensionBase(
                compilerConfiguration = compilerConfiguration,
                declarationFinder = { pluginContext ->
                    DeclarationFinderAdapter_2_2_0(
                        pluginContext
                    )
                },
                moduleTransformer = { ModuleTransformer_2_3_0(it) }
            )
        )
    }
}

open class ModuleTransformer_2_3_0(configuration: Configuration) : ModuleTransformer_2_2_0(configuration) {
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
}
