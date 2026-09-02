plugins {
    // These plugins compile code in build-logic. Their versions can differ from those
    // used to compile the project's Kotlin code elsewhere.
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.sam.with.receiver") version "2.2.0"
    kotlin("plugin.assignment") version "2.2.0"
    id("java-gradle-plugin")
}

dependencies {
    // These declarations load plugins into the root project, without applying them.
    // Doing so avoids issues when loading the same plugins in different subprojects, which will use separate
    // classloaders. In this case, plugins would not be able to communicate across projects via a shared build service.
    // Cf. https://discuss.gradle.org/t/why-duplicate-plugins-in-top-level-build-scripts/49087
    implementation(libs.com.android.gradle.plugin)
    implementation(libs.com.github.gmazzo.buildconfig.gradle.plugin)
    implementation(libs.com.gradleup.tapmoc.gradle.plugin)
    implementation(libs.com.vanniktech.maven.publish.gradle.plugin)
    implementation(libs.org.jetbrains.dokka.gradle.plugin)
    implementation(libs.org.jetbrains.kotlin.android.gradle.plugin)
    implementation(libs.org.jetbrains.kotlin.assignment.gradle.plugin)
    implementation(libs.org.jetbrains.kotlin.atomicfu.gradle.plugin)
    implementation(libs.org.jetbrains.kotlin.compose.gradle.plugin)
    implementation(libs.org.jetbrains.kotlin.gradle.plugin)
    implementation(libs.org.jetbrains.kotlin.sam.with.receiver.gradle.plugin)
    implementation(libs.org.jmailen.kotlinter.gradle.plugin)
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
            "kotlin-jvm" to "BuildLogicKotlinJvmPlugin",
            "kotlin-jvm-base" to "BuildLogicKotlinJvmBasePlugin",
            "kotlin-multiplatform" to "BuildLogicKotlinMultiplatformPlugin",
            "android-application" to "BuildLogicAndroidApplicationPlugin",
            "publishing" to "BuildLogicPublishingPlugin"
        )

        for ((id, implementationClass) in pluginMap) {
            register("buildLogic.$id") {
                this.id = "buildLogic.$id"
                this.implementationClass = implementationClass
            }
        }
    }
}
