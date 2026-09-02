pluginManagement {
    includeBuild("../../../build-settings")
    includeBuild("../../../build-logic")
    includeBuild("../gradle-plugin-layer-build-logic")
}

plugins {
    id("buildSettings")
}

dependencyResolutionManagement {
    versionCatalogs.create("libs") {
        from(files("../../../gradle/libs.versions.toml"))
    }
}
