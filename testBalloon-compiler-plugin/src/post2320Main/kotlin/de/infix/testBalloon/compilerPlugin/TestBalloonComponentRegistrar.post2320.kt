package de.infix.testBalloon.compilerPlugin

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.MessageCollectorAccess
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

actual fun CompilerPluginRegistrar.ExtensionStorage.registerFirExtensions(configuration: CompilerConfiguration) {
    FirExtensionRegistrarAdapter.registerExtension(
        TestBalloonFirExtensionRegistrar { session ->
            TestBalloonFirDeclarationGenerationExtension(session)
        }
    )
}

@OptIn(MessageCollectorAccess::class)
actual fun reportMessage(
    configuration: CompilerConfiguration,
    severity: CompilerMessageSeverity,
    message: String,
    sourceLocation: CompilerMessageSourceLocation?
) {
    val messageCollector = configuration.get(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
    messageCollector.report(severity, message, sourceLocation)
}
