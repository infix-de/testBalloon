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

fun RepositoryHandler.projectRepositories() {
    mavenCentral()
    System.getProperty("user.home")?.let { home ->
        maven(url = uri("$home/.m2/local-repository"))
    }
    maven(url = uri("https://packages.jetbrains.team/maven/p/compiler-plugin-dev-kit/eap"))
    gradlePluginPortal()
    maven("https://repo.gradle.org/gradle/libs-releases/")
}

dependencyResolutionManagement {
    versionCatalogs.create("libs") {
        from(files("../gradle/libs.versions.toml"))
    }
}
