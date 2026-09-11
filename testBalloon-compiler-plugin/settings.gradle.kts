pluginManagement {
    includeBuild("../build-settings")
    includeBuild("../build-logic")
}

plugins {
    id("buildSettings")
}

dependencyResolutionManagement {
    versionCatalogs.create("libs") {
        from(files("../gradle/libs.versions.toml"))
    }
}

include(":base-test")

val pluginLayers = listOf(
    "base",
    "kotlin-2-2-0",
    "kotlin-2-3-0",
    "kotlin-2-3-20",
    "kotlin-2-4-0",
    "kotlin-2-4-10"
)

for (pluginLayer in pluginLayers) {
    includeBuild("layer/$pluginLayer")
}
