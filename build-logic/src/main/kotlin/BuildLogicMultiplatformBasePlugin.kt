import buildLogic.withCompatPatrouille
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

/**
 * Build logic for Kotlin Multiplatform targets, excluding Android (AGP) targets.
 */
@Suppress("unused")
class BuildLogicMultiplatformBasePlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        withCompatPatrouille {
            apply("buildLogic.common")
            apply("org.jetbrains.kotlin.multiplatform")
            apply("org.jetbrains.kotlin.plugin.atomicfu")
        }

        val kotlin = extensions.getByName("kotlin") as KotlinMultiplatformExtension

        fun Task.kotlinSourceSets(name: String, predicate: (sourceSet: KotlinSourceSet) -> Boolean) {
            group = "help"
            description = "Prints Mermaid diagram code displaying the Kotlin $name source set hierarchy."
            notCompatibleWithConfigurationCache("This task references source sets by its nature.")

            doLast {
                println("\nclassDiagram")
                val testSourceSets = kotlin.sourceSets.filter { predicate(it) }
                for (sourceSet in testSourceSets) {
                    println("    class ${sourceSet.name}")
                }
                for (sourceSet in testSourceSets) {
                    for (dependsOnSourceSet in sourceSet.dependsOn.map { it.name }) {
                        println("    $dependsOnSourceSet <|-- ${sourceSet.name}")
                    }
                }
                println("\nYou can display the above diagram in https://mermaid.live/\n")
            }
        }

        tasks.register("kotlinMainSourceSets") {
            kotlinSourceSets("main") { !Regex("[tT]est").containsMatchIn(it.name) }
        }

        tasks.register("kotlinTestSourceSets") {
            kotlinSourceSets("test") { Regex("[tT]est").containsMatchIn(it.name) }
        }
    }
}
