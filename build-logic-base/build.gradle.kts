plugins {
    // These plugins compile code in build-logic-base. Their versions can differ from those
    // used to compile the project's Kotlin code elsewhere.
    kotlin("jvm") version "2.4.21"
    kotlin("plugin.sam.with.receiver") version "2.4.21"
    kotlin("plugin.assignment") version "2.4.21"
    id("java-gradle-plugin")
}

dependencies {
    // These declarations load plugins into the root project, without applying them.
    // Doing so avoids issues when loading the same plugins in different subprojects, which will use separate
    // classloaders. In this case, plugins would not be able to communicate across projects via a shared build service.
    // Cf. https://discuss.gradle.org/t/why-duplicate-plugins-in-top-level-build-scripts/49087
    implementation(libs.com.github.gmazzo.buildconfig.gradle.plugin)
    api(libs.com.gradleup.tapmoc.gradle.plugin)
    implementation(libs.org.jetbrains.dokka.gradle.plugin)
    implementation(libs.com.vanniktech.maven.publish.gradle.plugin)
    implementation(libs.org.jmailen.kotlinter.gradle.plugin)
    implementation(gradleApi())
}

samWithReceiver {
    annotation(HasImplicitReceiver::class.qualifiedName!!)
}

assignment {
    annotation(SupportsKotlinAssignmentOverloading::class.qualifiedName!!)
}

gradlePlugin {
    plugins {
        val pluginMap = mapOf(
            "common" to "BuildLogicCommonPlugin",
            "dokka" to "BuildLogicDokkaPlugin",
            "publishing" to "BuildLogicPublishingPlugin"
        )

        for ((id, implementationClass) in pluginMap) {
            register("buildLogic.base.$id") {
                this.id = "buildLogic.base.$id"
                this.implementationClass = "buildLogic.base.$implementationClass"
            }
        }
    }
}
