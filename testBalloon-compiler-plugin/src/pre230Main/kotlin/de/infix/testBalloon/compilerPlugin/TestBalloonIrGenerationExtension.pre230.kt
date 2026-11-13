package de.infix.testBalloon.compilerPlugin

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.util.addFakeOverrides

context(_: ModuleTransformer)
actual fun IrClass.addFakeOverridesCompatibly(typeSystem: IrTypeSystemContext) {
    addFakeOverrides(typeSystem)
}

context(transformer: ModuleTransformer)
actual fun registerReference(entryPointFile: IrFile, referencedDeclaration: IrDeclarationWithName) {
}
