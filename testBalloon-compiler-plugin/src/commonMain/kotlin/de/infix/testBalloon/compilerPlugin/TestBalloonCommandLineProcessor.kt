package de.infix.testBalloon.compilerPlugin

import de.infix.testBalloon.compilerPlugin.layer.base.Options
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.devkit.DevKitCLP
import org.jetbrains.kotlin.config.CompilerConfiguration

class TestBalloonCommandLineProcessor : DevKitCLP {
    override val pluginOptions: Collection<AbstractCliOption> = Options.all

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        val specificOption = optionsByName[option.optionName]
        if (specificOption != null) {
            specificOption.addToCompilerConfiguration(configuration, value)
        } else {
            throw IllegalArgumentException("Unexpected plugin option ${option.optionName}")
        }
    }

    companion object {
        private val optionsByName = Options.all.associateBy { it.optionName }
    }
}
