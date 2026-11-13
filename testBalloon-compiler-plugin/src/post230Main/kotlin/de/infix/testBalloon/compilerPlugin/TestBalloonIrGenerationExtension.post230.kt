package de.infix.testBalloon.compilerPlugin

import org.jetbrains.kotlin.backend.jvm.ir.fileParent
import org.jetbrains.kotlin.config.fileMappingTracker
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.util.addFakeOverrides
import java.io.File

context(_: ModuleTransformer)
actual fun IrClass.addFakeOverridesCompatibly(typeSystem: IrTypeSystemContext) {
    addFakeOverrides(typeSystem)
}

context(transformer: ModuleTransformer)
actual fun registerReference(entryPointFile: IrFile, referencedDeclaration: IrDeclarationWithName) {
    transformer.pluginContext.recordLookup(referencedDeclaration, entryPointFile)
    transformer.configuration.compilerConfiguration.fileMappingTracker?.recordSourceReferencedByCompilerPlugin(
        File(referencedDeclaration.fileParent.path)
    )
}
