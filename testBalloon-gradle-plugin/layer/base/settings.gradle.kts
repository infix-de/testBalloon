pluginManagement {
    includeBuild("../../../build-settings")
    includeBuild("../../../build-logic")
    includeBuild("../build-logic-g")
}

plugins {
    id("buildSettings")
}

dependencyResolutionManagement {
    versionCatalogs.create("libs") {
        from(files("../../../gradle/libs.versions.toml"))
    }
}
