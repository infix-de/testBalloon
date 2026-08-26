import buildLogic.libraryFromCatalog
import buildLogic.propagateLifecycleTasksToIncludedBuilds
import tapmoc.Severity

plugins {
    id("buildLogic.kotlin-jvm")
    alias(libs.plugins.org.jetbrains.kotlin.plugin.sam.with.receiver)
    alias(libs.plugins.org.jetbrains.kotlin.plugin.assignment)
    id("java-gradle-plugin")
    id("buildLogic.publishing")
    alias(libs.plugins.com.github.gmazzo.buildconfig)
}

description = "Gradle plugin for the TestBalloon framework"

dependencies {
    implementation(libs.org.jetbrains.kotlin.gradle.plugin)
    compileOnly(libs.com.android.gradle.plugin)
    implementation("$group:testBalloon-framework-shared:$version")
}

samWithReceiver {
    annotation(HasImplicitReceiver::class.qualifiedName!!)
}

assignment {
    annotation(SupportsKotlinAssignmentOverloading::class.qualifiedName!!)
}

tapmoc {
    // WORKAROUND Dependency validation fails on Gradle plugins – https://github.com/GradleUp/Tapmoc/issues/69
    checkJavaClassFiles(Severity.IGNORE)
}

buildConfig {
    packageName("buildConfig")
    useKotlinOutput { internalVisibility = true }

    buildConfigField(
        "String",
        "PROJECT_COMPILER_PLUGIN_ID",
        "\"${project.property("local.PROJECT_COMPILER_PLUGIN_ID")}\""
    )
    buildConfigField("String", "PROJECT_VERSION", "\"$version\"")
    buildConfigField("String", "PROJECT_GROUP_ID", "\"$group\"")
    buildConfigField(
        "String",
        "PROJECT_JUNIT_PLATFORM_LAUNCHER",
        "\"${libraryFromCatalog("org.junit.platform.launcher")}\""
    )
}

gradlePlugin {
    plugins {
        register("${project.property("local.PROJECT_COMPILER_PLUGIN_ID")}") {
            id = "${project.property("local.PROJECT_COMPILER_PLUGIN_ID")}"
            displayName = "TestBalloon compiler plugin for multiplatform test discovery"
            description = displayName
            implementationClass = "$group.gradlePlugin.TestBalloonGradlePlugin"
        }
    }
}

propagateLifecycleTasksToIncludedBuilds()
