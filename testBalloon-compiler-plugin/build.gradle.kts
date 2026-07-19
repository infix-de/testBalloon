@file:Suppress("UnstableApiUsage")

plugins {
    id("buildLogic.kotlin-jvm")
    id("buildLogic.publishing-jvm")
    id("com.github.gmazzo.buildconfig")
}

description = "Compiler plugin for the TestBalloon framework"

/** Dependencies to be embedded into the compiler plugin artifact. */
val embedded = configurations.dependencyScope("embedded")

/** The consumable configuration containing the resulting embedded elements. */
val embeddedResult = configurations.resolvable("embeddedResult") {
    extendsFrom(embedded)
    exclude(module = "kotlin-stdlib")
}

/** Adds a dependency to the `embedded` configuration. */
fun DependencyHandler.embedded(dependencyNotation: Any) = add(embedded.name, dependencyNotation)

@Suppress("AvoidDuplicateDependencies", "RedundantSuppression")
dependencies {
    // WORKAROUND https://youtrack.jetbrains.com/issue/KT-53477 – KGP misses transitive compiler plugin dependencies
    embedded(projects.testBalloonFrameworkShared)
    // TODO: Add version-specific modules here

    project.configurations.named("compileOnly").configure { extendsFrom(embedded) }
    compileOnly(libs.org.jetbrains.kotlin.stdlib)
    compileOnly(libs.org.jetbrains.kotlin.compiler.embeddable)

    project.configurations.named("testImplementation").configure { extendsFrom(embedded) }
    testImplementation(libs.org.jetbrains.kotlin.stdlib)
    testImplementation(libs.org.jetbrains.kotlin.compiler.embeddable)

    testImplementation(libs.dev.zacsweers.kctfork)
    testImplementation(libs.org.jetbrains.kotlin.test)
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

val integratedJar = tasks.register("integratedJar", Jar::class.java) {
    group = "build"
    description = "Creates the compiler plugin's integrated JAR, including embedded dependencies."

    // The following is required for CC compatibility (Project.zipTree(Object) cannot be used).
    val archiveOperations = run {
        abstract class InjectionTarget {
            @get:Inject
            abstract val archiveOperations: ArchiveOperations
        }
        project.objects.newInstance(InjectionTarget::class.java).archiveOperations
    }

    // Include the compiler plugin's classes.
    from(java.sourceSets.main.map { it.output })

    // Include the compiler plugin's embedded dependency classes, unzipping JARs.
    from(
        embeddedResult.map { embeddedResult ->
            embeddedResult.elements.map { location ->
                location.map { archiveOperations.zipTree(it.asFile) }
            }
        }
    )

    archiveClassifier = "integrated"
}

// Publish the integrated JAR instead of the default one.
// See https://github.com/vanniktech/gradle-maven-publish-plugin/issues/1123#issuecomment-3670312723
configurations {
    for (configurationName in listOf("runtimeElements", "apiElements")) {
        named(configurationName) {
            outgoing {
                artifacts.clear()
                artifact(integratedJar)
            }
        }
    }
}
