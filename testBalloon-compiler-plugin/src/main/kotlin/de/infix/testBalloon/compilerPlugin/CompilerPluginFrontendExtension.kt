@file:OptIn(TestBalloonInternalApi::class)

package de.infix.testBalloon.compilerPlugin

import de.infix.testBalloon.framework.shared.internal.Constants
import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalApi
import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.ExperimentalTopLevelDeclarationsGenerationApi
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.plugin.createTopLevelClass
import org.jetbrains.kotlin.fir.plugin.createTopLevelFunction
import org.jetbrains.kotlin.fir.plugin.createTopLevelProperty
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.platform.isWasm
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.platform.konan.isNative

@OptIn(ExperimentalTopLevelDeclarationsGenerationApi::class)
class CompilerPluginFrontendExtension(session: FirSession) : FirDeclarationGenerationExtension(session) {

    private companion object {
        object PluginKey : GeneratedDeclarationKey()

        val jvmEntryPointClassId = ClassId.topLevel(FqName(Constants.JVM_ENTRY_POINT_CLASS_NAME))
    }

    private val platform = session.moduleData.platform
    private val platformIsJsOrWasm = platform.isJs() || platform.isWasm()

    private var jvmEntryPointClassSymbol: FirClassSymbol<*>? = null

    override fun getTopLevelClassIds(): Set<ClassId> = setOf(jvmEntryPointClassId)

    override fun getTopLevelCallableIds(): Set<CallableId> = setOf(mainFunctionId, nativeEntryPointPropertyId)

    override fun hasPackage(packageFqName: FqName): Boolean = packageFqName == entryPointPackageFqName

    /**
     * Returns a top-level class `JvmEntryPoint` for JVM targets.
     */
    override fun generateTopLevelClassLikeDeclaration(classId: ClassId): FirClassLikeSymbol<*>? =
        if (platform.isJvm() && classId == jvmEntryPointClassId) {
            createTopLevelClass(classId = jvmEntryPointClassId, key = PluginKey) {
                visibility = Visibilities.Internal
            }.symbol.also {
                jvmEntryPointClassSymbol = it
            }
        } else {
            null
        }

    /**
     * Returns a static `testFrameworkDiscoveryResult` method for the top-level `class JvmEntryPoint` for JVM targets.
     */
    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> =
        if (platform.isJvm() && classSymbol == jvmEntryPointClassSymbol) {
            setOf(Name.identifier(Constants.JVM_DISCOVERY_RESULT_METHOD_NAME))
        } else {
            emptySet()
        }

    /**
     * Returns a top-level function `suspend fun main(): Unit` for JS and Wasm targets.
     */
    override fun generateFunctions(
        callableId: CallableId,
        context: MemberGenerationContext?
    ): List<FirNamedFunctionSymbol> = if (platformIsJsOrWasm && callableId == mainFunctionId) {
        val mainFunction = createTopLevelFunction(
            key = PluginKey,
            callableId = mainFunctionId,
            returnType = session.builtinTypes.unitType.coneType
        ) {
            visibility = Visibilities.Public
            status {
                isSuspend = true
            }
        }

        listOf(mainFunction.symbol)
    } else {
        emptyList()
    }

    /**
     * Returns a top-level property `val testFrameworkNativeEntryPoint: Unit` for Native targets.
     */
    override fun generateProperties(
        callableId: CallableId,
        context: MemberGenerationContext?
    ): List<FirPropertySymbol> {
        if (!platform.isNative() || callableId != nativeEntryPointPropertyId) return emptyList()

        val entryPointProperty = createTopLevelProperty(
            key = PluginKey,
            callableId = nativeEntryPointPropertyId,
            returnType = session.builtinTypes.unitType.coneType
        ) {
            visibility = Visibilities.Private
        }

        return listOf(entryPointProperty.symbol)
    }
}
