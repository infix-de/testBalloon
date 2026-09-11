package de.infix.testBalloon.compilerPlugin

import de.infix.testBalloon.compilerPlugin.buildConfig.BuildConfig.PROJECT_ROOT_GROUP
import de.infix.testBalloon.compilerPlugin.layer.base.CompilerAdapter
import de.infix.testBalloon.compilerPlugin.layer.base.Options
import de.infix.testBalloon.compilerPlugin.layer.base.PLUGIN_DISPLAY_NAME
import de.infix.testBalloon.compilerPlugin.layer.base.PLUGIN_ID
import de.infix.testBalloon.framework.shared.internal.DebugLevel
import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalApi
import de.infix.testBalloon.framework.shared.internal.asKotlinVersion
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

@OptIn(ExperimentalCompilerApi::class, TestBalloonInternalApi::class)
class CompilerPluginRegistrar : CompilerPluginRegistrar() {
    @Suppress("unused") // pluginId is an override property required for Kotlin versions >= 2.3.0
    val pluginId: String = PLUGIN_ID

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
                        "$PLUGIN_DISPLAY_NAME: [DEBUG] using compiler adapter ${this.configuration.adapterVersion}" +
                            " for Kotlin compiler ${this.configuration.compilerVersion}"
                    )
                }
                registerExtensions(configuration)
            }
        } else {
            if (debugLevel > DebugLevel.NONE) {
                reportMessage(
                    CompilerMessageSeverity.STRONG_WARNING,
                    "$PLUGIN_DISPLAY_NAME: [DEBUG] compiler plugin is disabled ($disablingReason)."
                )
            }
        }
    }

    private fun compilerAdapter(): CompilerAdapter {
        val compilerVersion =
            FirExtensionRegistrar::class.java.classLoader?.getResourceAsStream("META-INF/compiler.version")
                ?.bufferedReader()?.use { it.readText() }?.takeUnless { it.isBlank() }?.asKotlinVersion()
                ?: throw IllegalArgumentException("$PLUGIN_DISPLAY_NAME: Could not determine the compiler version.")

        for (adapterVersionString in listOf("2.4.0", "2.3.20", "2.3.0", "2.2.0")) {
            val adapterVersion = adapterVersionString.asKotlinVersion()
            if (adapterVersion <= compilerVersion) {
                val adapterConfiguration = CompilerAdapter.Configuration(pluginId, compilerVersion, adapterVersion)
                val packageVersion = adapterVersionString.replace(".", "")
                val classVersion = adapterVersionString.replace(".", "_")
                val className =
                    "${PROJECT_ROOT_GROUP}.compilerPlugin.layer.kotlin$packageVersion.CompilerAdapter_$classVersion"
                return Class.forName(className)
                    .getDeclaredConstructor(CompilerAdapter.Configuration::class.java)
                    .newInstance(adapterConfiguration) as CompilerAdapter
            }
        }

        throw NotImplementedError("$PLUGIN_DISPLAY_NAME: Kotlin compiler version '$compilerVersion' is unsupported.")
    }
}
