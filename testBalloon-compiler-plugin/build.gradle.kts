plugins {
    id("buildLogic.kotlin-jvm")
    id("buildLogic.publishing-jvm")
    id("com.github.gmazzo.buildconfig")
    id("com.gradleup.shadow")
}

description = "Compiler plugin for the TestBalloon framework"

@Suppress("AvoidDuplicateDependencies", "RedundantSuppression")
dependencies {
    // region - `implementation` dependencies are included in the shadow jar.

    // WORKAROUND https://youtrack.jetbrains.com/issue/KT-53477 – KGP misses transitive compiler plugin dependencies
    implementation(projects.testBalloonFrameworkShared)

    // TODO: Add version-specific modules here

    // endregion

    // region - `compileOnly` dependencies are excluded from the shadow jar.

    compileOnly(libs.org.jetbrains.kotlin.compiler.embeddable)
    compileOnly(libs.org.jetbrains.kotlinx.coroutines.core)
    compileOnly(libs.org.jetbrains.kotlin.stdlib)

    // endregion

    testImplementation(libs.dev.zacsweers.kctfork)
    testImplementation(libs.org.jetbrains.kotlin.test)

    // Not bundled in the shadow jar
    testImplementation(libs.org.jetbrains.kotlin.compiler.embeddable)
    testImplementation(libs.org.jetbrains.kotlinx.coroutines.core)
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
    buildConfigField("String", "PROJECT_FRAMEWORK_CORE_ARTIFACT_ID", "\"${projects.testBalloonFrameworkCore.name}\"")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

// Publish the shadow jar instead of the default non-shadow jar.
// WORKAROUND: The gradle-maven-publish-plugin does not support publishing the shadow plugin's artifacts.
//     https://github.com/vanniktech/gradle-maven-publish-plugin/issues/1123#issuecomment-3670312723
configurations {
    for (configurationName in listOf("runtimeElements", "apiElements")) {
        named(configurationName) {
            outgoing {
                artifacts.clear()
                artifact(tasks.shadowJar)
            }
        }
    }
}

tasks.shadowJar {
    minimize()
}
