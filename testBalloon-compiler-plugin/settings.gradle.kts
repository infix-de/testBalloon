pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://packages.jetbrains.team/maven/p/compiler-plugin-dev-kit/eap")
    }

    includeBuild("../build-settings")
    // includeBuild("../build-logic")
}

plugins {
    id("buildSettings")
    kotlin("compiler.plugin.devkit") version "0.0.4-dev-380"
}

dependencyResolutionManagement {
    versionCatalogs.create("libs") {
        from(files("../gradle/libs.versions.toml"))
    }
}

// include(":base-test")
