package de.infix.testBalloon.dokka

import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.renderers.html.NavigationNode
import org.jetbrains.dokka.base.renderers.html.NavigationPageInstaller
import org.jetbrains.dokka.base.renderers.html.transform
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import org.jetbrains.dokka.plugability.Extension
import org.jetbrains.dokka.plugability.PluginApiPreviewAcknowledgement
import org.jetbrains.dokka.transformers.pages.PageTransformer

/**
 * A Dokka plugin which hides extension functions and empty packages in the table of contents (side menu).
 */
class NavigationNodeHidingDokkaPlugin : DokkaPlugin() {
    @Suppress("unused")
    val myFilterExtension: Extension<PageTransformer, *, *> by extending {
        val dokkaBase = plugin<DokkaBase>()
        dokkaBase.htmlPreprocessors providing ::NavigationNodeHidingTransformer override
            dokkaBase.navigationPageInstaller
    }

    @DokkaPluginApiPreview
    override fun pluginApiPreviewAcknowledgement() = PluginApiPreviewAcknowledgement
}

private class NavigationNodeHidingTransformer(context: DokkaContext) : NavigationPageInstaller(context) {
    override fun navigableChildren(input: RootPageNode): NavigationNode = super.navigableChildren(input).transform {
        NavigationNode(
            it.name,
            it.dri,
            it.sourceSets,
            it.icon,
            it.styles,
            it.children.filterNot { child ->
                // Drop extension functions and empty packages.
                child.isExtensionFunction() || child.isEmptyPackage()
            }
        )
    }

    private fun NavigationNode.isExtensionFunction(): Boolean = dri.callable?.receiver != null

    private fun NavigationNode.isEmptyPackage(): Boolean {
        val isPackage = dri.classNames == null && dri.callable == null && dri.packageName != null
        return isPackage && children.isEmpty()
    }
}
