package de.infix.testBalloon.compilerPlugin

import de.infix.testBalloon.compilerPlugin.layer.base.Options
import de.infix.testBalloon.framework.shared.internal.DebugLevel
import de.infix.testBalloon.framework.shared.internal.KotlinVersion
import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalApi
import de.infix.testBalloon.framework.shared.internal.asKotlinVersion
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.devkit.DevKitComponentRegistrar
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

@OptIn(TestBalloonInternalApi::class)
class TestBalloonComponentRegistrar : DevKitComponentRegistrar {
    override fun CompilerPluginRegistrar.ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val compilerVersion: KotlinVersion =
            FirExtensionRegistrar::class.java.classLoader?.getResourceAsStream("META-INF/compiler.version")
                ?.bufferedReader()?.use { it.readText() }?.takeUnless { it.isBlank() }?.asKotlinVersion()
                ?: "0.0.0".asKotlinVersion()

        // AnalysisFlags.ideMode is reportedly not working with earlier IntelliJ versions.
        // See https://github.com/ZacSweers/metro/blob/d83f74a59a9728921d627c952e2aa476665590fc/compiler/src/main/kotlin/dev/zacsweers/metro/compiler/IdeModeChecks.kt#L31-L32.
        val isDefinitelyInvokedByIde =
            compilerVersion >= "2.4.20".asKotlinVersion() &&
                configuration.languageVersionSettings.getFlag(AnalysisFlags.ideMode)

        if (isDefinitelyInvokedByIde || compilerVersion.isForIntelliJIdea) {
            reportMessage(
                configuration,
                CompilerMessageSeverity.STRONG_WARNING,
                "$PLUGIN_DISPLAY_NAME: IDE invocation detected (compiler version $compilerVersion)" +
                    " – disabling the compiler plugin.",
                null
            )
            return
        }

        val disablingReason = Options.disablingReason.value(configuration)

        if (disablingReason.isNotEmpty()) {
            if (Options.debugLevel.value(configuration) > DebugLevel.NONE) {
                reportMessage(
                    configuration,
                    CompilerMessageSeverity.STRONG_WARNING,
                    "$PLUGIN_DISPLAY_NAME: [DEBUG] compiler plugin is disabled ($disablingReason).",
                    null
                )
            }
            return
        }

        registerFirExtensions(configuration)

        IrGenerationExtension.registerExtension(
            TestBalloonIrGenerationExtension(compilerConfiguration = configuration)
        )
    }
}

expect fun CompilerPluginRegistrar.ExtensionStorage.registerFirExtensions(configuration: CompilerConfiguration)
