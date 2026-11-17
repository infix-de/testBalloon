plugins {
    alias(libs.plugins.org.jetbrains.kotlin.jvm.latest.release)
    alias(libs.plugins.org.jetbrains.kotlin.plugin.sam.with.receiver.latest.release)
    alias(libs.plugins.org.jetbrains.kotlin.plugin.assignment.latest.release)
    id("java-gradle-plugin")
}

dependencies {
    // These declarations load plugins into the root project, without applying them.
    // Doing so avoids issues when loading the same plugins in different subprojects, which will use separate
    // classloaders. In this case, plugins would not be able to communicate across projects via a shared build service.
    // Cf. https://discuss.gradle.org/t/why-duplicate-plugins-in-top-level-build-scripts/49087
    implementation(libs.com.android.gradle.plugin)
    implementation(libs.com.gradleup.nmcp.aggregation.gradle.plugin)
    implementation(libs.com.gradleup.nmcp.gradle.plugin)
    implementation(libs.org.jetbrains.kotlin.android.gradle.plugin)
    implementation(libs.org.jetbrains.kotlin.assignment.gradle.plugin)
    implementation(libs.org.jetbrains.kotlin.atomicfu.gradle.plugin)
    implementation(libs.org.jetbrains.kotlin.compose.gradle.plugin)
    implementation(libs.org.jetbrains.kotlin.gradle.plugin)
    implementation(libs.org.jetbrains.kotlin.sam.with.receiver.gradle.plugin)
    implementation(":build-logic-base")
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
            "jvm" to "BuildLogicKotlinJvmPlugin",
            "jvm-base" to "BuildLogicKotlinJvmBasePlugin",
            "multiplatform" to "BuildLogicKotlinMultiplatformPlugin",
            "android-application" to "BuildLogicAndroidApplicationPlugin",
            "root-project" to "BuildLogicRootProjectPlugin"
        )

        for ((id, implementationClass) in pluginMap) {
            register("buildLogic.kotlin.$id") {
                this.id = "buildLogic.kotlin.$id"
                this.implementationClass = "buildLogic.kotlin.$implementationClass"
            }
        }
    }
}
