pluginManagement {
    includeBuild("../../../build-settings")
    includeBuild("../gradle-plugin-layer-build-logic")

    repositories {
        mavenCentral()
        maven(url = uri("https://packages.jetbrains.team/maven/p/compiler-plugin-dev-kit/eap"))
        gradlePluginPortal()
    }
}

plugins {
    id("buildSettings")
}

dependencyResolutionManagement {
    versionCatalogs.create("libs") {
        from(files("../../../gradle/libs.versions.toml"))
    }
}
