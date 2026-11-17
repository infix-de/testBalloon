pluginManagement {
    includeBuild("../build-settings")
    includeBuild("../build-logic-base")
}

plugins {
    id("buildSettings")
}

dependencyResolutionManagement {
    versionCatalogs.create("libs") {
        from(files("../gradle/libs.versions.toml"))
    }
}
