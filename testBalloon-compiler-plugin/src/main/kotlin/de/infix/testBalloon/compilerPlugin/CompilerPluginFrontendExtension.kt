@file:OptIn(TestBalloonInternalApi::class)

package de.infix.testBalloon.compilerPlugin

import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalApi
import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.ExperimentalTopLevelDeclarationsGenerationApi
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.plugin.createTopLevelFunction
import org.jetbrains.kotlin.fir.plugin.createTopLevelProperty
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName

@OptIn(ExperimentalTopLevelDeclarationsGenerationApi::class)
class CompilerPluginFrontendExtension(session: FirSession) : FirDeclarationGenerationExtension(session) {

    companion object {
        object Key : GeneratedDeclarationKey()

        var authoritativeExtensionFound = false
    }

    /**
     * Specifies whether this extension is authoritative to create top-level symbols.
     *
     * KMP creates multiple FIR sessions. Only the first one is authoritative. This avoids creating the same
     * symbol multiple times.
     */
    val isAuthoritative = if (!authoritativeExtensionFound) {
        authoritativeExtensionFound = true
        true
    } else {
        false
    }

    /**
     * Creates a top-level `suspend fun main()`, whose parameter list and body is to be initialized in IR.
     */
    override fun generateFunctions(
        callableId: CallableId,
        context: MemberGenerationContext?
    ): List<FirNamedFunctionSymbol> {
        if (!isAuthoritative || callableId != mainCallableId) return emptyList()

        val mainFunction = createTopLevelFunction(
            Key,
            mainCallableId,
            returnType = session.builtinTypes.unitType.coneType
        ) {
            visibility = Visibilities.Public
            status {
                isSuspend = true
            }
        }

        return listOf(mainFunction.symbol)
    }

    /**
     * Creates a top-level `val testFrameworkEntryPoint: Unit`, which is to be initialized in IR.
     */
    override fun generateProperties(
        callableId: CallableId,
        context: MemberGenerationContext?
    ): List<FirPropertySymbol> {
        if (!isAuthoritative || callableId != entryPointPropertyCallableId) return emptyList()

        val entryPointProperty = createTopLevelProperty(
            Key,
            entryPointPropertyCallableId,
            returnType = session.builtinTypes.unitType.coneType
        ) {
            visibility = Visibilities.Private
        }

        return listOf(entryPointProperty.symbol)
    }

    override fun getTopLevelCallableIds(): Set<CallableId> =
        if (isAuthoritative) setOf(mainCallableId, entryPointPropertyCallableId) else emptySet()

    override fun hasPackage(packageFqName: FqName): Boolean = packageFqName == entryPointPackageFqName
}
