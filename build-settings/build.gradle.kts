plugins {
    // These plugins compile code in build-settings. Their versions can differ from those
    // used to compile the project's Kotlin code elsewhere.
    kotlin("jvm") version "2.4.21"
    kotlin("plugin.sam.with.receiver") version "2.4.21"
    kotlin("plugin.assignment") version "2.4.21"
    id("com.gradleup.tapmoc") version "0.5.0"
    id("java-gradle-plugin")
}

tapmoc {
    gradle(gradle.gradleVersion)
}

dependencies {
    implementation(libs.de.fayard.refreshversions.gradle.plugin)
    implementation(libs.org.gradle.toolchains.foojay.resolver)
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
        register("buildSettings") {
            id = "buildSettings"
            implementationClass = "buildSettings.BuildSettingsPlugin"
        }
    }
}
