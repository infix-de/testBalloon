package de.infix.testBalloon.compilerPlugin

import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

actual fun CompilerPluginRegistrar.ExtensionStorage.registerFirExtensions(configuration: CompilerConfiguration) {
    FirExtensionRegistrarAdapter.registerExtension(
        TestBalloonFirExtensionRegistrar { session ->
            TestBalloonFirDeclarationGenerationExtension(session)
        }
    )
}
