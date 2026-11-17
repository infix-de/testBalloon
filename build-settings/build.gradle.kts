plugins {
    alias(libs.plugins.org.jetbrains.kotlin.jvm.latest.release)
    alias(libs.plugins.org.jetbrains.kotlin.plugin.sam.with.receiver.latest.release)
    alias(libs.plugins.org.jetbrains.kotlin.plugin.assignment.latest.release)
    alias(libs.plugins.com.gradleup.tapmoc)
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
