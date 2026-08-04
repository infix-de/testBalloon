package de.infix.testBalloon.compilerPlugin

import de.infix.testBalloon.compilerPlugin.base.CompilerAdapter
import de.infix.testBalloon.compilerPlugin.base.Options
import de.infix.testBalloon.compilerPlugin.base.PLUGIN_DISPLAY_NAME
import de.infix.testBalloon.compilerPlugin.base.PLUGIN_ID
import de.infix.testBalloon.compilerPlugin.base.asKotlinVersion
import de.infix.testBalloon.compilerPlugin.layer.kotlin230.CompilerAdapter_2_3_0
import de.infix.testBalloon.compilerPlugin.layer.kotlin2320.CompilerAdapter_2_3_20
import de.infix.testBalloon.compilerPlugin.layer.kotlin240.CompilerAdapter_2_4_0
import de.infix.testBalloon.framework.shared.internal.DebugLevel
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

@OptIn(ExperimentalCompilerApi::class)
class CompilerPluginRegistrar : CompilerPluginRegistrar() {
    @Suppress("unused") // pluginId is an override property required for Kotlin versions >= 2.3.0
    override val pluginId: String = PLUGIN_ID

    override val supportsK2 = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val messageCollector by lazy {
            configuration.get(
                CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY,
                MessageCollector.NONE
            )
        }

        fun reportMessage(severity: CompilerMessageSeverity, message: String) =
            messageCollector.report(severity, message)

        val disablingReason = Options.disablingReason.value(configuration)
        val debugLevel = Options.debugLevel.value(configuration)

        if (disablingReason.isEmpty()) {
            with(compilerAdapter()) {
                if (debugLevel > DebugLevel.NONE) {
                    reportMessage(
                        CompilerMessageSeverity.STRONG_WARNING,
                        "${PLUGIN_DISPLAY_NAME}: [DEBUG] using compiler adapter $adapterVersion" +
                            " for Kotlin compiler ${this.configuration.compilerVersion}"
                    )
                }
                registerExtensions(configuration)
            }
        } else {
            if (debugLevel > DebugLevel.NONE) {
                reportMessage(
                    CompilerMessageSeverity.STRONG_WARNING,
                    "${PLUGIN_DISPLAY_NAME}: [DEBUG] compiler plugin is disabled ($disablingReason)."
                )
            }
        }
    }

    private fun compilerAdapter(): CompilerAdapter {
        val compilerVersion =
            FirExtensionRegistrar::class.java.classLoader?.getResourceAsStream("META-INF/compiler.version")
                ?.bufferedReader()?.use { it.readText() }?.takeUnless { it.isBlank() }?.asKotlinVersion()
                ?: throw IllegalArgumentException("${PLUGIN_DISPLAY_NAME}: Could not determine the compiler version.")

        val adapterConfiguration = CompilerAdapter.Configuration(pluginId, compilerVersion)

        val adapters =
            listOf<(CompilerAdapter.Configuration) -> CompilerAdapter>(
                { CompilerAdapter_2_4_0(it) },
                { CompilerAdapter_2_3_20(it) },
                { CompilerAdapter_2_3_0(it) }
            )
        for (adapter in adapters) {
            val configuredAdapter = adapter(adapterConfiguration)
            if (configuredAdapter.adapterVersion <= compilerVersion) return configuredAdapter
        }

        throw NotImplementedError("${PLUGIN_DISPLAY_NAME}: Kotlin compiler version '$compilerVersion' is unsupported.")
    }
}
