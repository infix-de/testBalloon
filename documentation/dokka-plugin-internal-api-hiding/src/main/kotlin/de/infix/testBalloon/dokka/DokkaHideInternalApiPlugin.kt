package de.infix.testBalloon.dokka

import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalApi
import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalTestingApi
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.transformers.documentables.SuppressedByConditionDocumentableFilterTransformer
import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import org.jetbrains.dokka.plugability.PluginApiPreviewAcknowledgement

/**
 * A Dokka plugin which hides symbols annotated with `@TestBalloonInternalApi`.
 */
class InternalApiHidingDokkaPlugin : DokkaPlugin() {
    @Suppress("unused")
    val myFilterExtension by extending {
        plugin<DokkaBase>().preMergeDocumentableTransformer providing ::InternalApiHidingTransformer
    }

    @DokkaPluginApiPreview
    override fun pluginApiPreviewAcknowledgement() = PluginApiPreviewAcknowledgement
}

private class InternalApiHidingTransformer(context: DokkaContext) :
    SuppressedByConditionDocumentableFilterTransformer(context) {

    override fun shouldBeSuppressed(d: Documentable): Boolean {
        val annotations: List<Annotations.Annotation> =
            (d as? WithExtraProperties<*>)
                ?.extra
                ?.allOfType<Annotations>()
                ?.flatMap { it.directAnnotations.values.flatten() }
                ?: emptyList()

        return annotations.any { it.fqn() in internalApiAnnotationFqn }
    }

    private fun Annotations.Annotation.fqn() = "${dri.packageName}.${dri.classNames}"

    companion object {
        val internalApiAnnotationFqn =
            setOf(TestBalloonInternalApi::class.qualifiedName, TestBalloonInternalTestingApi::class.qualifiedName)
    }
}
