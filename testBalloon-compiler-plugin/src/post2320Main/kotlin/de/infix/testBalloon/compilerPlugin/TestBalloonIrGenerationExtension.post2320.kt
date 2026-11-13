package de.infix.testBalloon.compilerPlugin

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.ir.builders.irAnnotation
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrMutableAnnotationContainer
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
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
    annotations +=
        irTargetSymbol.irAnnotation(
            transformer.irConstructorCall(irTargetSymbol.owner, irAnnotationClassSymbol).symbol
        ).apply {
            annotationArguments.forEachIndexed { index, annotationArgument ->
                arguments[index] = annotationArgument
            }
        }
}

context(transformer: ModuleTransformer)
private fun IrSymbol.irAnnotation(irClassSymbol: IrConstructorSymbol) =
    transformer.pluginContext.irBuiltIns.createIrBuilder(this).irAnnotation(irClassSymbol, emptyList())

actual val defaultPropertyAccessor: IrDeclarationOrigin
    get() = IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR

actual fun IrPluginContext.findClass(classId: ClassId): IrClassSymbol? = finderForBuiltins().findClass(classId)

actual fun IrPluginContext.findFunctions(callableId: CallableId): Collection<IrSimpleFunctionSymbol> =
    finderForBuiltins().findFunctions(callableId)

actual fun IrPluginContext.findProperties(callableId: CallableId): Collection<IrPropertySymbol> =
    finderForBuiltins().findProperties(callableId)
