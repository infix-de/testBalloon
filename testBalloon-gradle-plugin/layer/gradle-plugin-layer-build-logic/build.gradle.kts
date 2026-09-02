plugins {
    // These plugins compile code in build-logic. Their versions can differ from those
    // used to compile the project's Kotlin code elsewhere.
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.sam.with.receiver") version "2.2.0"
    kotlin("plugin.assignment") version "2.2.0"
    id("java-gradle-plugin")
}

samWithReceiver {
    annotation(HasImplicitReceiver::class.qualifiedName!!)
}

assignment {
    annotation(SupportsKotlinAssignmentOverloading::class.qualifiedName!!)
}

dependencies {
    // These declarations load plugins into the root project, without applying them.
    // Doing so avoids issues when loading the same plugins in different subprojects, which will use separate
    // classloaders. In this case, plugins would not be able to communicate across projects via a shared build service.
    // Cf. https://discuss.gradle.org/t/why-duplicate-plugins-in-top-level-build-scripts/49087
    implementation(libs.com.gradleup.tapmoc.gradle.plugin)
    implementation(libs.org.jmailen.kotlinter.gradle.plugin)
    implementation(libs.org.jetbrains.kotlin.sam.with.receiver.gradle.plugin)
    implementation(libs.org.jetbrains.kotlin.assignment.gradle.plugin)
}

gradlePlugin {
    plugins {
        val pluginMap = mapOf("common" to "BuildLogicCommonPlugin")

        for ((id, implementationClass) in pluginMap) {
            register("gradlePlugin.layer.buildLogic.$id") {
                this.id = "gradlePlugin.layer.buildLogic.$id"
                this.implementationClass = "gradlePlugin.layer.buildLogic.$implementationClass"
            }
        }
    }
}
