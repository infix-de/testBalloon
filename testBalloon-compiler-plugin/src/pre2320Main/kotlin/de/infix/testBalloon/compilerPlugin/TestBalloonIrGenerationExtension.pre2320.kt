package de.infix.testBalloon.compilerPlugin

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrMutableAnnotationContainer
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId

context(transformer: ModuleTransformer)
actual fun IrMutableAnnotationContainer.addAnnotation(
    irTargetSymbol: IrSymbol,
    irAnnotationClassSymbol: IrClassSymbol,
    vararg annotationArguments: IrExpression
) {
    annotations += transformer.irConstructorCall(irTargetSymbol.owner, irAnnotationClassSymbol, *annotationArguments)
}

actual val defaultPropertyAccessor: IrDeclarationOrigin
    get() = IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR

actual fun IrPluginContext.findClass(classId: ClassId): IrClassSymbol? = referenceClass(classId)

actual fun IrPluginContext.findFunctions(callableId: CallableId): Collection<IrSimpleFunctionSymbol> =
    referenceFunctions(callableId)

actual fun IrPluginContext.findProperties(callableId: CallableId): Collection<IrPropertySymbol> =
    referenceProperties(callableId)
