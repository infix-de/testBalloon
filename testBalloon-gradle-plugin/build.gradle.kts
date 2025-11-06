import buildLogic.libraryFromCatalog

plugins {
    id("buildLogic.jvm")
    id("buildLogic.publishing-jvm")
    id("org.jetbrains.kotlin.plugin.sam.with.receiver")
    id("org.jetbrains.kotlin.plugin.assignment")
    id("com.github.gmazzo.buildconfig")
    id("java-gradle-plugin")
}

description = "Gradle plugin for the TestBalloon framework"

dependencies {
    implementation(libs.org.jetbrains.kotlin.gradle.plugin)
    implementation(projects.testBalloonFrameworkShared)
}

samWithReceiver {
    annotation(HasImplicitReceiver::class.qualifiedName!!)
}

assignment {
    annotation(SupportsKotlinAssignmentOverloading::class.qualifiedName!!)
}

buildConfig {
    packageName("buildConfig")
    useKotlinOutput { internalVisibility = true }

    buildConfigField(
        "String",
        "PROJECT_COMPILER_PLUGIN_ID",
        "\"${project.property("local.PROJECT_COMPILER_PLUGIN_ID")}\""
    )
    buildConfigField("String", "PROJECT_VERSION", "\"${project.version}\"")
    buildConfigField("String", "PROJECT_GROUP_ID", "\"${project.group}\"")
    buildConfigField("String", "PROJECT_COMPILER_PLUGIN_ARTIFACT_ID", "\"${projects.testBalloonCompilerPlugin.name}\"")
    buildConfigField(
        "String",
        "PROJECT_SHARED_ARTIFACT_ID",
        "\"${projects.testBalloonFrameworkShared.name}\""
    )
    buildConfigField(
        "String",
        "PROJECT_JUNIT_PLATFORM_LAUNCHER",
        "\"${libraryFromCatalog("org.junit.platform.launcher")}\""
    )
}

gradlePlugin {
    plugins {
        create("testBalloonGradlePlugin") {
            id = "${project.property("local.PROJECT_COMPILER_PLUGIN_ID")}"
            displayName = "TestBalloon compiler plugin for multiplatform test discovery"
            description = displayName
            implementationClass = "${project.group}.gradlePlugin.TestBalloonGradlePlugin"
        }
    }
}
