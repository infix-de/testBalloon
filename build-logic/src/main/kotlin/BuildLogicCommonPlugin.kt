import buildLogic.jdkVersion
import buildLogic.kotlinVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jmailen.gradle.kotlinter.KotlinterExtension
import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.LintTask

@Suppress("unused")
class BuildLogicCommonPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        with(pluginManager) {
            apply("com.gradleup.tapmoc")
            apply("org.jmailen.kotlinter")
        }

        group = project.property("local.PROJECT_GROUP_ID")!!

        val isStdlibDefaultDependencyEnabled =
            project.findProperty("kotlin.stdlib.default.dependency")?.toString()?.toBooleanStrictOrNull() != false

        extensions.configure<tapmoc.TapmocExtension>("tapmoc") {
            java(jdkVersion())
            if (isStdlibDefaultDependencyEnabled) {
                // Configure the Kotlin version for regular projects, but prevent Tapmoc from pulling in a stdlib
                // dependency where it must be avoided (the compiler plugin modules).
                kotlin(kotlinVersion())
            }
            checkDependencies(tapmoc.Severity.ERROR)
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
