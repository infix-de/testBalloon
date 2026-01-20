@file:OptIn(TestBalloonInternalApi::class)

package de.infix.testBalloon.compilerPlugin

import buildConfig.BuildConfig.PROJECT_COMPILER_PLUGIN_ID
import buildConfig.BuildConfig.PROJECT_FRAMEWORK_CORE_ARTIFACT_ID
import buildConfig.BuildConfig.PROJECT_GROUP_ID
import buildConfig.BuildConfig.PROJECT_VERSION
import de.infix.testBalloon.framework.shared.AbstractTestSession
import de.infix.testBalloon.framework.shared.AbstractTestSuite
import de.infix.testBalloon.framework.shared.TestDisplayName
import de.infix.testBalloon.framework.shared.TestElementName
import de.infix.testBalloon.framework.shared.TestRegistering
import de.infix.testBalloon.framework.shared.internal.Constants
import de.infix.testBalloon.framework.shared.internal.DebugLevel
import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalApi
import de.infix.testBalloon.framework.shared.internal.TestFrameworkDiscoveryResult
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.fileParent
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.IrSingleStatementBuilder
import org.jetbrains.kotlin.ir.builders.Scope
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addGetter
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irVararg
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.createBlockBody
import org.jetbrains.kotlin.ir.declarations.name
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.addChild
import org.jetbrains.kotlin.ir.util.addFakeOverrides
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.copyTypeAndValueArgumentsFrom
import org.jetbrains.kotlin.ir.util.createThisReceiverParameter
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isSubtypeOfClass
import org.jetbrains.kotlin.ir.util.packageFqName
import org.jetbrains.kotlin.ir.util.toIrConst
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.platform.isWasm
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.platform.konan.isNative
import kotlin.reflect.KClass

class CompilerPluginIrGenerationExtension(private val compilerConfiguration: CompilerConfiguration) :
    IrGenerationExtension {

    private val messageCollector =
        compilerConfiguration.get(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val debugLevel = Options.debugLevel.value(compilerConfiguration)
        val testModuleRegex = Options.testModuleRegex.value(compilerConfiguration)

        if (debugLevel > DebugLevel.NONE) {
            messageCollector.report(
                CompilerMessageSeverity.WARNING,
                "$PLUGIN_DISPLAY_NAME: [DEBUG] Plugin version $PROJECT_VERSION is processing" +
                    " module ${moduleFragment.name}."
            )
        }

        fun reportDisablingReason(detail: String) {
            if (debugLevel > DebugLevel.NONE) {
                messageCollector.report(
                    CompilerMessageSeverity.WARNING,
                    "$PLUGIN_DISPLAY_NAME: [DEBUG] Disabling the plugin for module ${moduleFragment.name}: $detail"
                )
            }
        }

        // If we are not compiling a test module: Disable the compiler plugin.
        // Otherwise, we end up defining the discovery result property twice (one for the main module, another one
        // for the test module). If the test module picks up the main module's symbol, no suites will be considered
        // discovered.
        if (!Regex(testModuleRegex).containsMatchIn(moduleFragment.name.asStringStripSpecialMarkers())) {
            reportDisablingReason("It is not a test module (matching '$testModuleRegex').")
            return
        }

        // If we are not compiling a module with the framework library dependency: Disable the compiler plugin.
        if (!ModuleProbe(pluginContext, messageCollector).hasFrameworkLibraryDependency()) {
            reportDisablingReason("It has no framework library dependency.")
            return
        }

        val configuration: Configuration = try {
            Configuration(compilerConfiguration, pluginContext, messageCollector)
        } catch (exception: MissingFrameworkSymbol) {
            messageCollector.report(CompilerMessageSeverity.ERROR, "$PLUGIN_DISPLAY_NAME: ${exception.message}")
            return
        }

        moduleFragment.transform(ModuleTransformer(pluginContext, messageCollector, configuration), null)
    }
}

private class ModuleProbe(
    override val pluginContext: IrPluginContext,
    override val messageCollector: MessageCollector
) : ModuleWideSymbolResolving {
    /** Returns true if the currently compiled module is `TestSuite`-aware. */
    fun hasFrameworkLibraryDependency(): Boolean = irClassSymbolOrNull(AbstractTestSuite::class.qualifiedName!!) != null
}

private class Configuration(
    compilerConfiguration: CompilerConfiguration,
    override val pluginContext: IrPluginContext,
    override val messageCollector: MessageCollector
) : ModuleWideSymbolResolving {

    val coreInternalPackageName = Constants.CORE_INTERNAL_PACKAGE_NAME

    val debugLevel = Options.debugLevel.value(compilerConfiguration)
    val junit4AutoIntegrationEnabled = Options.junit4AutoIntegrationEnabled.value(compilerConfiguration)

    val abstractSuiteSymbol = irClassSymbol(AbstractTestSuite::class)
    val abstractSessionSymbol = irClassSymbol(AbstractTestSession::class)
    val testRegisteringAnnotationSymbol = irClassSymbol(TestRegistering::class)
    val testElementNameAnnotationSymbol = irClassSymbol(TestElementName::class)
    val testDisplayNameAnnotationSymbol = irClassSymbol(TestDisplayName::class)

    @OptIn(TestBalloonInternalApi::class)
    val testFrameworkDiscoveryResultClassSymbol by lazy { irClassSymbol(TestFrameworkDiscoveryResult::class) }

    val initializeTestFrameworkFunctionSymbol by lazy {
        irFunctionSymbol(coreInternalPackageName, "initializeTestFramework")
    }
    val setUpAndExecuteTestsFunctionSymbol by lazy {
        irFunctionSymbol(coreInternalPackageName, "setUpAndExecuteTests")
    }
    val setUpAndExecuteTestsBlockingFunctionSymbol by lazy {
        irFunctionSymbol(coreInternalPackageName, "setUpAndExecuteTestsBlocking")
    }

    val jvmEntryPointClassSymbol by lazy { irClassSymbol(Constants.JVM_ENTRY_POINT_CLASS_NAME) }

    val jUnit4RunWithAnnotationSymbol by lazy {
        irClassSymbolOrNull("org.junit.runner.RunWith")
    }

    val testBalloonJUnit4RunnerSymbol by lazy { irClassSymbolOrNull(Constants.JUNIT4_RUNNER_CLASS_NAME) }

    val testBalloonJUnit4EntryPointName = Name.identifier(Constants.JUNIT4_ENTRY_POINT_SIMPLE_CLASS_NAME)
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
private class ModuleTransformer(
    override val pluginContext: IrPluginContext,
    override val messageCollector: MessageCollector,
    val configuration: Configuration
) : IrElementTransformerVoidWithContext(),
    ModuleWideSymbolResolving {

    class DiscoveredSuite(
        val referencedDeclaration: IrDeclarationWithName,
        val valueExpression: IrBuilderWithScope.() -> IrExpression
    )

    val discoveredSuites = mutableListOf<DiscoveredSuite>()
    var customSessionClass: IrClass? = null

    override var sourceFileForReporting: IrFile? = null

    override fun visitFileNew(declaration: IrFile): IrFile {
        @Suppress("UnnecessaryVariable", "RedundantSuppression")
        val irFile = declaration

        sourceFileForReporting = irFile

        if (configuration.debugLevel >= DebugLevel.BASIC) {
            reportDebug("Analyzing source file", irFile)
        }

        // For debugging only: Print the IR code from 'DumpIr.kt' anywhere in the module.
        if (configuration.debugLevel >= DebugLevel.CODE && irFile.name == "DumpIr.kt") {
            reportDebug("Dump from file '${irFile.name}':\n${irFile.dump().prependIndent("\t")}")
        }

        return super.visitFileNew(irFile)
    }

    override fun visitClassNew(declaration: IrClass): IrStatement {
        @Suppress("UnnecessaryVariable", "RedundantSuppression")
        val irClass = declaration

        withErrorReporting(irClass, "Could not analyze class '${irClass.fqName()}'") {
            // Fast path: Top-level classes only.
            if (irClass.parent !is IrFile) return@withErrorReporting

            if (irClass.isSameOrSubTypeOf(configuration.abstractSessionSymbol)) {
                if (customSessionClass == null) {
                    if (configuration.debugLevel >= DebugLevel.DISCOVERY) {
                        reportDebug("Found test session '${irClass.fqName()}'", irClass)
                    }
                    customSessionClass = irClass
                } else {
                    reportError(
                        "Found multiple test sessions annotated with" +
                            " @${configuration.testRegisteringAnnotationSymbol.owner.name}," +
                            " but expected at most one.",
                        irClass
                    )
                }
            }
        }

        return super.visitClassNew(irClass)
    }

    override fun visitPropertyNew(declaration: IrProperty): IrStatement {
        @Suppress("UnnecessaryVariable", "RedundantSuppression")
        val irProperty = declaration

        withErrorReporting(irProperty, "Could not analyze property '${irProperty.fqNameWhenAvailable}'") {
            // Fast path: Top-level delegating properties only.
            if (!(irProperty.isDelegated && irProperty.parent is IrFile)) return@withErrorReporting

            // Look for an initialization via a function call.
            val initializer = irProperty.backingField?.initializer ?: return@withErrorReporting
            val initializerCall = initializer.expression as? IrCall ?: return@withErrorReporting
            val initializerCallFunction = initializerCall.symbol.owner

            if (initializerCallFunction.hasAnnotation(configuration.testRegisteringAnnotationSymbol)) {
                if (configuration.debugLevel >= DebugLevel.DISCOVERY) {
                    reportDebug("Found top-level test suite property '${irProperty.fqNameWhenAvailable}'", irProperty)
                }

                irProperty.addNameValueArgumentsToInitializerCallIfApplicable(
                    initializer,
                    initializerCall,
                    initializerCallFunction
                )

                discoveredSuites.add(DiscoveredSuite(irProperty) { irCall(irProperty.getter!!.symbol) })
            }
        }

        return super.visitPropertyNew(declaration)
    }

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        // Process the entire module fragment first, collecting all test suites.
        val moduleFragment = super.visitModuleFragment(declaration)

        // We have left all source files behind.
        sourceFileForReporting = null

        withErrorReporting(
            moduleFragment,
            "Could not generate entry point code"
        ) {
            if (configuration.debugLevel >= DebugLevel.CODE) {
                reportDebug(
                    "Generating code in module '${moduleFragment.name}'," +
                        " for  ${discoveredSuites.size} discovered top-level suites," +
                        " custom session: " +
                        if (customSessionClass == null) "default" else "${customSessionClass?.fqName()}",
                    moduleFragment
                )
            }

            val platform = pluginContext.platform
            val entryPointFile: IrFile
            when {
                platform.isJvm() -> {
                    entryPointFile = irJvmEntryPointClass().fileParent
                    irJUnit4RunnerEntryPointClass(entryPointFile)?.let { entryPointFile.addChild(it) }
                }

                platform.isJs() || platform.isWasm() -> {
                    entryPointFile = irSuspendMainFunction().fileParent
                }

                platform.isNative() -> {
                    entryPointFile = irTestFrameworkEntryPointProperty().fileParent
                }

                else -> throw UnsupportedOperationException("Cannot generate entry points for platform '$platform'")
            }

            if (configuration.debugLevel >= DebugLevel.CODE) {
                reportDebug("Generated:\n${declaration.dump().prependIndent("\t")}")
            }

            // With incremental compilation, the compiler needs to recompile the entry point file if
            // - one of its referenced declarations change, or
            // - a new declaration to be referenced appears (either in a new source or by changing an existing source).
            // To do so, it needs to be told about such references.
            // We register the entry point file referencing
            // - the custom session class (if available), and
            // - top-level suites.
            customSessionClass?.let { registerReference(entryPointFile, it) }
            for (discoveredSuite in discoveredSuites) {
                registerReference(entryPointFile, discoveredSuite.referencedDeclaration)
            }
        }

        return moduleFragment
    }

    /**
     * Adds value arguments for element and display name to [this] property's initializer function call, if applicable.
     *
     * Parameters are added according to `@[TestElementName]` and `@[TestDisplayName]` annotations.
     */
    private fun IrProperty.addNameValueArgumentsToInitializerCallIfApplicable(
        initializer: IrExpressionBody,
        initializerCall: IrCall,
        initializerCallFunction: IrSimpleFunction
    ) {
        val irProperty = this
        val valueParameters = initializerCallFunction.parameters
        val valueArguments = initializerCall.arguments

        val nameValueArgumentsToAdd = nameValueArgumentsToAdd(
            mapOf(
                configuration.testElementNameAnnotationSymbol to { irProperty.fqName() },
                configuration.testDisplayNameAnnotationSymbol to { "${irProperty.name}" }
            ),
            valueParameters,
            valueArguments
        )

        if (nameValueArgumentsToAdd.isEmpty()) return

        initializer.transformChildren(
            object : IrElementTransformerVoid() {
                var callProcessed = false

                override fun visitCall(expression: IrCall): IrExpression {
                    // Fast path: Skip all calls after the first one.
                    if (callProcessed) return super.visitCall(expression)
                    callProcessed = true

                    @Suppress("UnnecessaryVariable", "RedundantSuppression")
                    val originalCall = expression
                    return DeclarationIrBuilder(
                        pluginContext,
                        currentScope!!.scope.scopeOwnerSymbol,
                        originalCall.startOffset,
                        originalCall.endOffset
                    ).run {
                        @Suppress("DuplicatedCode")
                        irCall(originalCall.symbol).apply {
                            copyTypeAndValueArgumentsFrom(originalCall)
                            nameValueArgumentsToAdd.forEach { (index, value) ->
                                arguments[index] = irString(value)
                                if (configuration.debugLevel >= DebugLevel.CODE) {
                                    reportDebug(
                                        "${irProperty.fqName()}: Setting parameter '${valueParameters[index].name}'" +
                                            " to '$value'"
                                    )
                                }
                            }
                        }
                    }
                }
            },
            null
        )
    }

    /**
     * Returns an (index -> value) map of value arguments to add for element and display name.
     *
     * [generatedValuesByAnnotation] specifies which annotation translates to which generated argument value.
     * Annotated candidates in a call's [valueParameters], which are missing a [valueArguments], generate
     * a value argument in the result map.
     */
    private fun nameValueArgumentsToAdd(
        generatedValuesByAnnotation: Map<IrClassSymbol, () -> String>,
        valueParameters: List<IrValueParameter>,
        valueArguments: List<IrExpression?>
    ): Map<Int, String> = valueParameters.mapNotNull { valueParameter ->
        generatedValuesByAnnotation.firstNotNullOfOrNull { (annotationSymbol, value) ->
            // Annotated parameters with a missing value argument only.
            if (valueParameter.hasAnnotation(annotationSymbol) &&
                valueArguments[valueParameter.indexInParameters] == null
            ) {
                Pair(valueParameter.indexInParameters, value())
            } else {
                null
            }
        }
    }.toMap()

    private fun IrProperty.fqName(): String = fqNameWhenAvailable.toString()

    /**
     * Returns a completed `main` function for [discoveredSuites] returning s1...sn:
     *
     * ```
     * suspend fun main(arguments: Array<String>) {
     *     initializeTestFramework(customSessionOrNull, arguments)
     *     setUpAndExecuteTests(arrayOf(s1, ..., sn))
     * }
     * ```
     */
    private fun irSuspendMainFunction(): IrSimpleFunction {
        val symbol = irFunctionSymbol(mainFunctionId)

        with(symbol.owner) {
            val irArgumentsValueParameter = addValueParameter(
                "args",
                pluginContext.irBuiltIns.arrayClass.typeWith(pluginContext.irBuiltIns.stringType),
                origin
            )
            body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
                +irSimpleFunctionCall(
                    configuration.initializeTestFrameworkFunctionSymbol,
                    customSessionClass?.let { irConstructorCall(it.symbol) },
                    irGet(irArgumentsValueParameter)
                )
                +irSimpleFunctionCall(
                    configuration.setUpAndExecuteTestsFunctionSymbol,
                    irArrayOfRootSuites()
                )
            }

            return this
        }
    }

    /**
     * Returns a `testFrameworkNativeEntryPoint` property for [discoveredSuites] returning s1...sn:
     *
     * ```
     * @EagerInitialization
     * private val testFrameworkNativeEntryPoint: Unit = run {
     *     initializeTestFramework(customSessionOrNull)
     *     setUpAndExecuteTestsBlocking(arrayOf(s1, ..., sn))
     * }
     * ```
     */
    private fun irTestFrameworkEntryPointProperty(): IrProperty {
        val symbol = irPropertySymbol(nativeEntryPointPropertyId)

        with(symbol.owner) {
            annotations += irConstructorCall(irClassSymbol("kotlin.native.EagerInitialization"))

            initializeWith(nativeEntryPointPropertyId.callableName, pluginContext.irBuiltIns.unitType) {
                +irSimpleFunctionCall(
                    configuration.initializeTestFrameworkFunctionSymbol,
                    customSessionClass?.let { irConstructorCall(it.symbol) }
                )
                +irSimpleFunctionCall(
                    configuration.setUpAndExecuteTestsBlockingFunctionSymbol,
                    irArrayOfRootSuites()
                )
            }

            return this
        }
    }

    /**
     * Returns a completed `JvmEntryPoint` class for [discoveredSuites] s1...sn:
     *
     * ```
     * companion object {
     *     internal fun testFrameworkDiscoveryResult(): TestFrameworkDiscoveryResult {
     *         initializeTestFramework(customSessionOrNull)
     *         return TestFrameworkDiscoveryResult(arrayOf(s1, ..., sn))
     *     }
     * }
     * ```
     */
    private fun irJvmEntryPointClass(): IrDeclaration {
        val classSymbol = configuration.jvmEntryPointClassSymbol

        with(classSymbol.owner) {
            addFunction(
                name = Constants.JVM_DISCOVERY_RESULT_METHOD_NAME,
                returnType = configuration.testFrameworkDiscoveryResultClassSymbol.defaultType,
                visibility = DescriptorVisibilities.INTERNAL,
                isStatic = true
            ).apply {
                parent = classSymbol.owner
                annotations += irConstructorCall(
                    irClassSymbol(JvmName::class.qualifiedName!!),
                    Constants.JVM_DISCOVERY_RESULT_METHOD_NAME.toIrConst(pluginContext.irBuiltIns.stringType)
                )

                body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
                    +irSimpleFunctionCall(
                        configuration.initializeTestFrameworkFunctionSymbol,
                        customSessionClass?.let { irConstructorCall(it.symbol) }
                    )
                    +IrReturnImpl(
                        UNDEFINED_OFFSET,
                        UNDEFINED_OFFSET,
                        configuration.testFrameworkDiscoveryResultClassSymbol.defaultType,
                        symbol,
                        irConstructorCall(
                            configuration.testFrameworkDiscoveryResultClassSymbol,
                            irArrayOfRootSuites()
                        )
                    )
                }
            }

            return this
        }
    }

    /**
     * Returns an entry point class making JUnit 4 invoke TestBalloon's JUnit 4 runner, or null outside JUnit 4.
     *
     * ```
     * @RunWith(TestBalloonJUnit4Runner::class)
     * internal class TestBalloonJUnit4
     * ```
     */
    private fun irJUnit4RunnerEntryPointClass(entryPointFile: IrFile): IrDeclaration? {
        if (configuration.debugLevel > DebugLevel.NONE) {
            val jUnit4Found = configuration.jUnit4RunWithAnnotationSymbol != null
            val testBalloonJUnit4RunnerFound = configuration.jUnit4RunWithAnnotationSymbol != null
            reportDebug(
                when {
                    jUnit4Found && testBalloonJUnit4RunnerFound -> {
                        if (configuration.junit4AutoIntegrationEnabled) {
                            "Integrating with JUnit 4"
                        } else {
                            "Suppressing JUnit 4 auto-integration (not enabled)"
                        }
                    }

                    jUnit4Found -> "JUnit 4 is not on the classpath"

                    else -> "JUnit 4 is on the classpath, but the TestBalloon JUnit 4 runner is not"
                }
            )
        }

        if (!configuration.junit4AutoIntegrationEnabled) return null
        val jUnit4RunWithAnnotationSymbol = configuration.jUnit4RunWithAnnotationSymbol ?: return null
        val testBalloonJUnit4RunnerSymbol = configuration.testBalloonJUnit4RunnerSymbol ?: return null

        return pluginContext.irFactory.buildClass {
            name = configuration.testBalloonJUnit4EntryPointName
            kind = ClassKind.CLASS
            visibility = DescriptorVisibilities.INTERNAL
        }.apply {
            val irClass = this
            val irBuiltIns = pluginContext.irBuiltIns

            parent = entryPointFile
            superTypes = listOf(irBuiltIns.anyType)

            createThisReceiverParameter()

            addConstructor {
                isPrimary = true
                visibility = DescriptorVisibilities.PUBLIC
                returnType = symbol.defaultType
            }.apply {
                val irBuilder = DeclarationIrBuilder(pluginContext, symbol, startOffset, endOffset)
                body = irBuilder.irBlockBody {
                    +irDelegatingConstructorCall(irBuiltIns.anyClass.owner.constructors.single())
                    +IrInstanceInitializerCallImpl(startOffset, endOffset, irClass.symbol, irBuiltIns.unitType)
                }
            }

            addFakeOverrides(IrTypeSystemContextImpl(irBuiltIns))

            annotations +=
                irConstructorCall(
                    jUnit4RunWithAnnotationSymbol,
                    IrClassReferenceImpl(
                        startOffset = UNDEFINED_OFFSET,
                        endOffset = UNDEFINED_OFFSET,
                        type = irBuiltIns.kClassClass.typeWith(testBalloonJUnit4RunnerSymbol.defaultType),
                        symbol = testBalloonJUnit4RunnerSymbol,
                        classType = testBalloonJUnit4RunnerSymbol.defaultType
                    )
                )
        }
    }

    /**
     * Initializes a top-level val property returning [resultType] with a backing field and [initialization] statements.
     */
    private fun IrProperty.initializeWith(
        propertyName: Name,
        resultType: IrType,
        initialization: IrBlockBuilder.() -> Unit
    ) {
        val property = this

        val field = pluginContext.irFactory.buildField {
            name = propertyName
            type = resultType
            visibility = DescriptorVisibilities.PRIVATE
            isFinal = true
            isExternal = false
            isStatic = true // a top-level val must be static
        }.apply {
            parent = property.parent
            correspondingPropertySymbol = property.symbol
            initializer = pluginContext.irFactory.createExpressionBody(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                DeclarationIrBuilder(pluginContext, symbol).irBlock {
                    initialization()
                }
            )
        }

        backingField = field

        addGetter {
            origin = IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
            returnType = field.type
        }.apply {
            body = factory.createBlockBody(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                listOf(
                    IrReturnImpl(
                        UNDEFINED_OFFSET,
                        UNDEFINED_OFFSET,
                        pluginContext.irBuiltIns.nothingType,
                        symbol,
                        IrGetFieldImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, field.symbol, field.type)
                    )
                )
            )
        }
    }

    /**
     * Returns an array expression containing the list of results from [discoveredSuites].
     */
    private fun IrBuilderWithScope.irArrayOfRootSuites(): IrExpression {
        val irElementType = configuration.abstractSuiteSymbol.owner.defaultType
        val irArrayType = pluginContext.irBuiltIns.arrayClass.typeWith(irElementType)

        return irCall(
            callee = pluginContext.irBuiltIns.arrayOf,
            type = irArrayType,
            typeArguments = listOf(irElementType)
        ).apply {
            val irSuitesVararg: List<IrExpression> = discoveredSuites.map { discoveredSuite ->
                discoveredSuite.valueExpression.invoke(this@irArrayOfRootSuites)
            }
            arguments[0] = irVararg(irElementType, irSuitesVararg)
        }
    }

    private fun IrSymbolOwner.irConstructorCall(irClassSymbol: IrClassSymbol, vararg irValues: IrExpression?) =
        IrSingleStatementBuilder(pluginContext, Scope(symbol), UNDEFINED_OFFSET, UNDEFINED_OFFSET).build {
            irConstructorCall(irClassSymbol, *irValues)
        }

    private fun IrBuilderWithScope.irConstructorCall(
        irClassSymbol: IrClassSymbol,
        vararg irValues: IrExpression?
    ): IrConstructorCall {
        val irConstructor = irClassSymbol.constructors.singleOrNull()
            ?: throw IllegalArgumentException("$irClassSymbol must have a single constructor")

        return irCall(irConstructor).apply {
            irValues.forEachIndexed { index, irValue ->
                arguments[index] = irValue ?: irNull()
            }
        }
    }

    /**
     * Registers a reference from [entryPointFile] to [referencedDeclaration], residing in another file.
     *
     * This is required for incremental compilation.
     */
    @Suppress("unused")
    private fun registerReference(entryPointFile: IrFile, referencedDeclaration: IrDeclarationWithName) {
        // WORKAROUND: IC in Kotlin < 2.3.20-Beta1 does not support compiler plugins generating top-level declarations
    }

    fun IrClass.isSameOrSubTypeOf(irSupertypeClassSymbol: IrClassSymbol): Boolean =
        symbol.owner.defaultType.isSubtypeOfClass(irSupertypeClassSymbol)
}

private fun IrBuilderWithScope.irSimpleFunctionCall(
    irFunctionSymbol: IrSimpleFunctionSymbol,
    vararg irValues: IrExpression?
) = irCall(irFunctionSymbol).apply {
    irValues.forEachIndexed { index, irValue ->
        arguments[index] = irValue ?: irNull()
    }
}

/**
 * A module-wide symbol-resolving capability.
 *
 * "Module-wide" in this context means that resolved symbols are referenced on behalf of the entire compilation module.
 * For incremental compilation purposes, if such a symbol is invalidated, the entire module must be recompiled.
 */
private interface ModuleWideSymbolResolving : Reporting {
    val pluginContext: IrPluginContext

    fun irClassSymbol(kClass: KClass<*>): IrClassSymbol = irClassSymbol(kClass.qualifiedName!!)

    fun irClassSymbolOrNull(fqName: String): IrClassSymbol? =
        pluginContext.referenceClass(ClassId.topLevel(FqName(fqName)))

    fun irClassSymbol(fqName: String): IrClassSymbol = irClassSymbolOrNull(fqName)
        ?: throw MissingFrameworkSymbol("class '$fqName'")

    fun irFunctionSymbol(packageName: String, functionName: String): IrSimpleFunctionSymbol =
        irFunctionSymbol(CallableId(FqName(packageName), Name.identifier(functionName)))

    fun irFunctionSymbol(callableId: CallableId): IrSimpleFunctionSymbol =
        pluginContext.referenceFunctions(callableId).singleOrElse {
            if (it.isEmpty()) {
                throw MissingFrameworkSymbol("function '${callableId.asFqNameForDebugInfo()}'")
            } else {
                reportWarning(
                    "Function '${callableId.asFqNameForDebugInfo()}' found ${it.size} times\n" +
                        "\tThis may be caused by a misconfiguration of the module's dependencies."
                )
                it.first()
            }
        }

    fun irPropertySymbol(callableId: CallableId): IrPropertySymbol =
        pluginContext.referenceProperties(callableId).singleOrElse {
            if (it.isEmpty()) {
                throw MissingFrameworkSymbol("property '${callableId.asFqNameForDebugInfo()}'")
            } else {
                reportWarning(
                    "Property '${callableId.asFqNameForDebugInfo()}' found ${it.size} times\n" +
                        "\tThis may be caused by a misconfiguration of the module's dependencies."
                )
                it.first()
            }
        }
}

private interface Reporting {
    val messageCollector: MessageCollector
    val sourceFileForReporting: IrFile? get() = null

    fun <Result> withErrorReporting(declaration: IrElement, failureDescription: String, block: () -> Result): Result =
        try {
            block()
        } catch (throwable: Throwable) {
            report(CompilerMessageSeverity.EXCEPTION, "$failureDescription: $throwable", declaration)
            throw throwable
        }

    fun reportDebug(message: String, declaration: IrElement? = null) =
        report(CompilerMessageSeverity.WARNING, "[DEBUG] $message", declaration)

    fun reportWarning(message: String, declaration: IrElement? = null) =
        report(CompilerMessageSeverity.WARNING, message, declaration)

    fun reportError(message: String, declaration: IrElement? = null) =
        report(CompilerMessageSeverity.ERROR, message, declaration)

    fun report(severity: CompilerMessageSeverity, message: String, declaration: IrElement? = null) {
        fun IrFile.locationOrNull(offset: Int?): CompilerMessageLocation? {
            if (offset == null) return null
            val lineNumber = fileEntry.getLineNumber(offset) + 1
            val columnNumber = fileEntry.getColumnNumber(offset) + 1
            return CompilerMessageLocation.create(fileEntry.name, lineNumber, columnNumber, null)
        }

        messageCollector.report(
            severity,
            "$PLUGIN_DISPLAY_NAME: $message",
            sourceFileForReporting?.locationOrNull(declaration?.startOffset)
        )
    }
}

private class MissingFrameworkSymbol(typeAndName: String) :
    Error(
        "Could not find $typeAndName.\n" +
            "\tPlease add the dependency '$PROJECT_GROUP_ID:$PROJECT_FRAMEWORK_CORE_ARTIFACT_ID:$PROJECT_VERSION'."
    )

private fun IrClass.fqName(): String = "${packageFqName.asQualificationPrefix()}$name"

private fun FqName?.asQualificationPrefix(): String = if (this == null || isRoot) "" else "$this."

private fun <T> Collection<T>.singleOrElse(alternative: (collection: Collection<T>) -> T): T =
    singleOrNull() ?: alternative(this)

private const val PLUGIN_DISPLAY_NAME = "Plugin $PROJECT_COMPILER_PLUGIN_ID"
