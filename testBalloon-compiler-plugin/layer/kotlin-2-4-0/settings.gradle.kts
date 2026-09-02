pluginManagement {
    includeBuild("../../../build-settings")
    includeBuild("../compiler-plugin-layer-build-logic")
}

plugins {
    id("buildSettings")
}

dependencyResolutionManagement {
    versionCatalogs.create("libs") {
        from(files("../../../gradle/libs.versions.toml"))
    }
}
