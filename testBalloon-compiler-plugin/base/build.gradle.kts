plugins {
    id("buildLogic.kotlin-jvm-base")
    id("com.github.gmazzo.buildconfig")
}

description = "TestBalloon compiler plugin compatibility layer (base)"

dependencies {
    api(projects.testBalloonFrameworkShared)
    compileOnly(libs.org.jetbrains.kotlin.compiler)

    testImplementation(libs.org.jetbrains.kotlin.test)
    testImplementation(libs.io.kotest.assertions.core)
}

buildConfig {
    packageName("buildConfig")
    useKotlinOutput { internalVisibility = true }

    buildConfigField("String", "PROJECT_VERSION", "\"${project.version}\"")
    buildConfigField("String", "PROJECT_GROUP_ID", "\"${project.group}\"")
    buildConfigField("String", "PROJECT_FRAMEWORK_CORE_ARTIFACT_ID", "\"${projects.testBalloonFrameworkCore.name}\"")

    buildConfigField(
        "String",
        "PROJECT_COMPILER_PLUGIN_ID",
        "\"${project.property("local.PROJECT_COMPILER_PLUGIN_ID")}\""
    )
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
