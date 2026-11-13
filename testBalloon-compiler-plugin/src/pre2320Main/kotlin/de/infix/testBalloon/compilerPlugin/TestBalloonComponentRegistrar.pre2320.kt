package de.infix.testBalloon.compilerPlugin

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration

actual fun CompilerPluginRegistrar.ExtensionStorage.registerFirExtensions(configuration: CompilerConfiguration) {
}

actual fun reportMessage(
    configuration: CompilerConfiguration,
    severity: CompilerMessageSeverity,
    message: String,
    sourceLocation: CompilerMessageSourceLocation?
) {
    val messageCollector = configuration.get(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
    messageCollector.report(severity, message, sourceLocation)
}
