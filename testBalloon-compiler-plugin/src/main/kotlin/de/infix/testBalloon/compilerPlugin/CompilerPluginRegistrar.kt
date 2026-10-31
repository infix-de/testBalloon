package de.infix.testBalloon.compilerPlugin

import de.infix.testBalloon.compilerPlugin.buildConfig.BuildConfig.PROJECT_ROOT_GROUP
import de.infix.testBalloon.compilerPlugin.layer.base.CompilerAdapter
import de.infix.testBalloon.compilerPlugin.layer.base.Options
import de.infix.testBalloon.compilerPlugin.layer.base.PLUGIN_DISPLAY_NAME
import de.infix.testBalloon.compilerPlugin.layer.base.PLUGIN_ID
import de.infix.testBalloon.framework.shared.internal.DebugLevel
import de.infix.testBalloon.framework.shared.internal.KotlinVersion
import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalApi
import de.infix.testBalloon.framework.shared.internal.asKotlinVersion
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

@OptIn(ExperimentalCompilerApi::class, TestBalloonInternalApi::class)
class CompilerPluginRegistrar : CompilerPluginRegistrar() {
    @Suppress("unused") // pluginId is an override property required for Kotlin versions >= 2.3.0
    val pluginId: String = PLUGIN_ID

    override val supportsK2 = true

    private val compilerVersion: KotlinVersion? by lazy {
        FirExtensionRegistrar::class.java.classLoader?.getResourceAsStream("META-INF/compiler.version")
            ?.bufferedReader()?.use { it.readText() }?.takeUnless { it.isBlank() }?.asKotlinVersion()
    }

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val messageCollector by lazy {
            configuration.get(
                CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY,
                MessageCollector.NONE
            )
        }

        fun reportMessage(severity: CompilerMessageSeverity, message: String) =
            messageCollector.report(severity, message)

        val compilerVersion = compilerVersion ?: run {
            reportMessage(
                CompilerMessageSeverity.STRONG_WARNING,
                "$PLUGIN_DISPLAY_NAME: Could not determine the compiler version – disabling the compiler plugin."
            )
            return
        }

        val compilerAdapter = compilerAdapter() ?: run {
            reportMessage(
                CompilerMessageSeverity.STRONG_WARNING,
                "$PLUGIN_DISPLAY_NAME: Could not determine compatibility with compiler version $compilerVersion" +
                    " – disabling the compiler plugin."
            )
            return
        }

        // AnalysisFlags.ideMode is reportedly not working with earlier IntelliJ versions.
        // See https://github.com/ZacSweers/metro/blob/d83f74a59a9728921d627c952e2aa476665590fc/compiler/src/main/kotlin/dev/zacsweers/metro/compiler/IdeModeChecks.kt#L31-L32.
        val isDefinitelyInvokedByIde = compilerVersion >= "2.4.20".asKotlinVersion() &&
            configuration.languageVersionSettings.getFlag(AnalysisFlags.ideMode)

        if (isDefinitelyInvokedByIde || compilerVersion.isForIntelliJIdea) {
            reportMessage(
                CompilerMessageSeverity.STRONG_WARNING,
                "$PLUGIN_DISPLAY_NAME: IDE invocation detected (compiler version $compilerVersion)" +
                    " – disabling the compiler plugin."
            )
            return
        }

        val disablingReason = Options.disablingReason.value(configuration)
        val debugLevel = Options.debugLevel.value(configuration)

        if (disablingReason.isEmpty()) {
            with(compilerAdapter) {
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

    private fun compilerAdapter(): CompilerAdapter? {
        val compilerVersion = compilerVersion ?: return null

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

        return null
    }
}
