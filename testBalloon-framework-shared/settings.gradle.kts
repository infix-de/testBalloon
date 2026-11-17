pluginManagement {
    includeBuild("../build-settings")
    includeBuild("../build-logic-kotlin")
}

plugins {
    id("buildSettings")
}

dependencyResolutionManagement {
    versionCatalogs.create("libs") {
        from(files("../gradle/libs.versions.toml"))
    }
}
