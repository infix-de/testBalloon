import kotlin.io.path.div
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

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

includeBuild("layer/base")

for (layer in (rootDir.toPath() / "layer").listDirectoryEntries().filter { it.name.startsWith("kotlin-") }) {
    includeBuild(layer.toString())
}
