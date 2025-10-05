@file:OptIn(TestBalloonInternalApi::class)

package de.infix.testBalloon.compilerPlugin

import de.infix.testBalloon.framework.internal.DebugLevel
import de.infix.testBalloon.framework.internal.TestBalloonInternalApi
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toUpperCaseAsciiOnly

internal object Options {
    val debugLevel = Option(
        optionName = "debugLevel",
        valueDescription = "DebugLevel",
        description = "Enable debug level (one of ${DebugLevel.entries})",
        defaultValue = DebugLevel.NONE
    ) { stringValue ->
        try {
            DebugLevel.valueOf(stringValue.toUpperCaseAsciiOnly())
        } catch (_: IllegalArgumentException) {
            throwValueError(stringValue)
        }
    }

    val jvmStandalone = Option(
        optionName = "jvmStandalone",
        valueDescription = "boolean",
        description = "Enable standalone invocation without JUnit Platform on the JVM",
        defaultValue = false
    ) { stringValue ->
        stringValue.toBooleanStrictOrNull() ?: throwValueError(stringValue)
    }

    val testModuleRegex = Option(
        optionName = "testModuleRegex",
        valueDescription = "string",
        description = "Regular expression qualifying a test module name",
        defaultValue = """(_test|Test)$"""
    ) { it }

    val all = registeredOptions.toList()
}

internal class Option<Type : Any>(
    override val optionName: String,
    override val valueDescription: String,
    override val description: String,
    override val required: Boolean = false,
    override val allowMultipleOccurrences: Boolean = false,
    val defaultValue: Type,
    val valueFromString: Option<Type>.(stringValue: String) -> Type
) : AbstractCliOption {
    private val key = CompilerConfigurationKey<Type>(optionName)

    init {
        registeredOptions.add(this)
    }

    fun value(compilerConfiguration: CompilerConfiguration): Type = compilerConfiguration[key] ?: defaultValue

    fun addToCompilerConfiguration(configuration: CompilerConfiguration, value: String) {
        configuration.put(key, valueFromString(value))
    }

    fun throwValueError(value: String): Nothing =
        throw IllegalArgumentException("Unexpected $valueDescription value '$value' for option '$optionName'")
}

private val registeredOptions = mutableListOf<Option<*>>()
