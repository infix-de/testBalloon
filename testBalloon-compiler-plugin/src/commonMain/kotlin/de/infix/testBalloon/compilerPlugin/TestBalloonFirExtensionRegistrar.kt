package de.infix.testBalloon.compilerPlugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.moduleData

class TestBalloonFirExtensionRegistrar(
    val firDeclarationGenerationExtension: (session: FirSession) -> FirDeclarationGenerationExtension
) : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +FirDeclarationGenerationExtension.Factory { session ->
            val isTargetTestModule = !session.moduleData.isCommon

            if (isTargetTestModule) {
                firDeclarationGenerationExtension(session)
            } else {
                NoOpFirDeclarationGenerationExtension(session)
            }
        }
    }

    private class NoOpFirDeclarationGenerationExtension(session: FirSession) :
        FirDeclarationGenerationExtension(session)
}
