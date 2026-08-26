plugins {
    id("buildLogic.kotlin-jvm-base")
    id("com.github.gmazzo.buildconfig")
}

description = "TestBalloon compiler plugin compatibility layer (base)"

dependencies {
    api("$group:testBalloon-framework-shared:$version")
    compileOnly(libs.org.jetbrains.kotlin.compiler)

    testImplementation(libs.org.jetbrains.kotlin.test)
    testImplementation(libs.io.kotest.assertions.core)
}

buildConfig {
    packageName("buildConfig")
    useKotlinOutput { internalVisibility = true }

    buildConfigField("String", "PROJECT_VERSION", "\"$version\"")
    buildConfigField("String", "PROJECT_GROUP_ID", "\"$group\"")
    buildConfigField("String", "PROJECT_FRAMEWORK_CORE_ARTIFACT_ID", "\"testBalloon-framework-core\"")

    buildConfigField(
        "String",
        "PROJECT_COMPILER_PLUGIN_ID",
        "\"${project.property("local.PROJECT_COMPILER_PLUGIN_ID")}\""
    )
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
