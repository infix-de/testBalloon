package de.infix.testBalloon.compilerPlugin

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.config.CompilerConfiguration

expect fun reportMessage(
    configuration: CompilerConfiguration,
    severity: CompilerMessageSeverity,
    message: String,
    sourceLocation: CompilerMessageSourceLocation?
)
