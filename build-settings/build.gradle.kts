plugins {
    // These plugins compile code in build-settings. Their versions can differ from those
    // used to compile the project's Kotlin code elsewhere.
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.sam.with.receiver") version "2.2.0"
    kotlin("plugin.assignment") version "2.2.0"
    id("java-gradle-plugin")
}

dependencies {
    implementation(libs.de.fayard.refreshversions.gradle.plugin)
    implementation(libs.org.gradle.toolchains.foojay.resolver)
}

samWithReceiver {
    annotation(HasImplicitReceiver::class.qualifiedName!!)
}

assignment {
    annotation(SupportsKotlinAssignmentOverloading::class.qualifiedName!!)
}

gradlePlugin {
    plugins {
        register("buildSettings") {
            id = "buildSettings"
            implementationClass = "BuildSettingsPlugin"
        }
    }
}
