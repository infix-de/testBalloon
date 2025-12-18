import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.plugins.signing.Sign

@Suppress("unused")
class BuildLogicPublishingJvmPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        with(pluginManager) {
            apply("buildLogic.publishing-base")
            apply("buildLogic.dokka")
        }

        extensions.configure<MavenPublishBaseExtension>("mavenPublishing") {
            configure(KotlinJvm(JavadocJar.Dokka("dokkaGenerateHtml"), sourcesJar = true))
        }
        // See https://github.com/gradle/gradle/issues/26091
        tasks.withType(AbstractPublishToMaven::class.java).configureEach {
            val signingTasks = tasks.withType(Sign::class.java)
            mustRunAfter(signingTasks)
        }

        // https://github.com/gradle/gradle/issues/26132
        afterEvaluate {
            tasks.configureEach {
                if (name.startsWith("compileTestKotlin")) {
                    val target = name.substring("compileTestKotlin".length)
                    val sign = try {
                        tasks.named("sign${target}Publication")
                    } catch (e: Throwable) {
                        null
                    }
                    if (sign != null) {
                        dependsOn(sign)
                    }
                }
            }
        }


    }
}
