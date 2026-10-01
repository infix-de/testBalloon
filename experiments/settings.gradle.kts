pluginManagement {
    includeBuild("../build-settings")
    includeBuild("../build-logic")
}

plugins {
    id("buildSettings")
}

includeBuild("../testBalloon-framework-shared")

dependencyResolutionManagement {
    versionCatalogs.create("libs") {
        from(files("../gradle/libs.versions.toml"))
    }
}
