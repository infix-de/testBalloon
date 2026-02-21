import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.dokka.gradle.DokkaExtension

@Suppress("unused")
class BuildLogicDokkaPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        with(pluginManager) {
            apply("org.jetbrains.dokka")
        }

        extensions.configure<DokkaExtension>("dokka") {
            dokkaPublications.named("html").configure {
                suppressInheritedMembers.set(true)
                failOnWarning.set(true)
            }

            dokkaSourceSets.configureEach {
                // includes.from("README.md")
                sourceLink {
                    localDirectory.set(rootDir)
                    remoteUrl("https://github.com/infix-de/testBalloon/tree/main")
                }
            }
        }

        dependencies.add("dokkaPlugin", project(":documentation:dokka-plugin-internal-api-hiding"))
    }
}

fun Project.dokkaEnableNavigationNodeHiding() =
    dependencies.add("dokkaPlugin", project(":documentation:dokka-plugin-navigation-node-hiding"))
