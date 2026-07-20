import buildLogic.addKotlinStdlibDependency
import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("unused")
class BuildLogicKotlinJvmPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        with(pluginManager) {
            apply("buildLogic.kotlin-jvm-base")
        }

        addKotlinStdlibDependency()
    }
}
