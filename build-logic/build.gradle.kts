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
    implementation(libs.org.jetbrains.kotlin.gradle.plugin)
    implementation(libs.org.jetbrains.kotlin.android.gradle.plugin)
    implementation(libs.com.android.gradle.plugin)
    implementation(libs.org.jetbrains.kotlin.atomicfu.gradle.plugin)
    implementation(libs.org.jetbrains.kotlin.dokka.gradle.plugin)
    implementation(libs.org.jetbrains.kotlinx.kover.gradle.plugin)
    implementation(libs.org.jmailen.kotlinter.gradle.plugin)
    implementation(libs.org.jetbrains.kotlin.atomicfu.gradle.plugin)
    implementation(libs.com.vanniktech.maven.publish.gradle.plugin)
    implementation(libs.com.gradleup.compat.patrouille.gradle.plugin)
    implementation(libs.com.github.gmazzo.buildconfig.gradle.plugin)
    implementation(libs.org.jetbrains.kotlin.sam.with.receiver.gradle.plugin)
    implementation(libs.org.jetbrains.kotlin.assignment.gradle.plugin)
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
            "buildLogic.common" to "BuildLogicCommonPlugin",
            "buildLogic.jvm" to "BuildLogicJvmPlugin",
            "buildLogic.multiplatform-base" to "BuildLogicMultiplatformBasePlugin",
            "buildLogic.multiplatform" to "BuildLogicMultiplatformPlugin",
            "buildLogic.multiplatform-excluding-wasm-wasi" to "BuildLogicMultiplatformExcludingWasmWasiPlugin",
            "buildLogic.multiplatform-plus-android-library" to "BuildLogicMultiplatformPlusAndroidLibraryPlugin",
            "buildLogic.multiplatform-plus-android-application" to "BuildLogicMultiplatformAndroidApplicationPlugin",
            "buildLogic.android-application" to "BuildLogicAndroidApplicationPlugin",
            "buildLogic.publishing-base" to "BuildLogicPublishingBasePlugin",
            "buildLogic.publishing-jvm" to "BuildLogicPublishingJvmPlugin",
            "buildLogic.publishing-multiplatform" to "BuildLogicPublishingMultiplatformPlugin"
        )

        for ((id, implementationClass) in pluginMap) {
            register(id) {
                this.id = id
                this.implementationClass = implementationClass
            }
        }
    }
}

val syncSharedTestBalloonSources by tasks.registering(Sync::class) {
    into(layout.buildDirectory.dir("generated/sharedTestBalloon/src/main"))
    from(
        layout.projectDirectory.dir(
            "../testBalloon-framework-abstractions/src/commonMain/kotlin/de/infix/testBalloon/framework/internal"
        )
    ) {
        include("AbstractTestElementPath.kt", "TestBalloonInternalApi.kt")
    }
    from(
        layout.projectDirectory.dir(
            "../testBalloon-gradle-plugin/src/main/kotlin/de/infix/testBalloon/gradlePlugin/shared"
        )
    )
}

kotlin {
    sourceSets {
        main {
            kotlin.srcDir(syncSharedTestBalloonSources)
        }
    }
}
