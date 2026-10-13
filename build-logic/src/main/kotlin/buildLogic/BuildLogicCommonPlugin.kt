package buildLogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.jmailen.gradle.kotlinter.KotlinterExtension
import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.LintTask
import com.gradleup.tapmoc.Severity
import com.gradleup.tapmoc.TapmocExtension

@Suppress("unused")
class BuildLogicCommonPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        with(pluginManager) {
            apply("com.gradleup.tapmoc")
            apply("org.jmailen.kotlinter")
        }

        group = rootGroup

        val extension = BuildLogicCommonExtension(target)
        extensions.add("buildLogicCommon", extension)

        extensions.configure<TapmocExtension>("tapmoc") {
            java(baseJdkVersion())
            kotlin(kotlinVersion())
            checkKotlinStdlibs(extension.kotlinStdlibSeverity.orElse(Severity.ERROR))
            checkJavaClassFiles(extension.javaClassFilesSeverity.orElse(Severity.ERROR))
            checkKotlinMetadata(extension.kotlinMetadataSeverity.orElse(Severity.ERROR))
        }

        extensions.configure<KotlinterExtension>("kotlinter") {
            ignoreLintFailures = false
            reporters = arrayOf("checkstyle", "plain")
        }

        tasks.withType(LintTask::class.java).configureEach {
            source = source.minus(fileTree("build")).asFileTree
        }

        tasks.withType(FormatTask::class.java).configureEach {
            source = source.minus(fileTree("build")).asFileTree
        }
    }
}

class BuildLogicCommonExtension(project: Project) {
    val javaClassFilesSeverity: Property<Severity> = project.objects.property(Severity::class.java)
    val kotlinMetadataSeverity: Property<Severity> = project.objects.property(Severity::class.java)
    val kotlinStdlibSeverity: Property<Severity> = project.objects.property(Severity::class.java)
}