import kotlin.io.path.div
import kotlin.io.path.listDirectoryEntries

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

include(":base")
include(":base-test")

for (layer in (rootDir.toPath() / "layer").listDirectoryEntries()) {
    includeBuild(layer.toString())
}
