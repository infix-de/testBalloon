pluginManagement {
    includeBuild("../build-settings")
    includeBuild("../build-logic")
    includeBuild("layer/gradle-plugin-layer-build-logic")
}

plugins {
    id("buildSettings")
}

dependencyResolutionManagement {
    versionCatalogs.create("libs") {
        from(files("../gradle/libs.versions.toml"))
    }
}

val pluginLayers = listOf(
    "kotlin-2-2-0",
    "kotlin-2-3-20",
    "kotlin-2-4-20"
)

for (pluginLayer in pluginLayers) {
    includeBuild("layer/$pluginLayer")
}
