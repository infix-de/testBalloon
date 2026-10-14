pluginManagement {
    includeBuild("../../build-settings")
}

plugins {
    id("buildSettings")
}

dependencyResolutionManagement {
    dependencyResolutionManagement {
        pluginManagement {
            repositories {
                projectRepositories()
            }
        }
        @Suppress("UnstableApiUsage")
        repositories {
            projectRepositories()
        }
    }
}

fun RepositoryHandler.projectRepositories() {
    maven(url = uri("../../build/integration-test-repository"))
    maven(url = uri("https://redirector.kotlinlang.org/maven/dev"))
}
