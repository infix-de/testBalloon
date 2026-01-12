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
    maven("https://redirector.kotlinlang.org/maven/dev")
    maven("https://packages.jetbrains.team/maven/p/kt/dev/org/jetbrains/kotlin/kotlin-compiler/")
    // Note: The 'dev' repo is unstable, releases are deleted after (two?) weeks.
    // The stable Kotlin pre-release repo is: https://packages.jetbrains.team/maven/p/kt/bootstrap
    gradlePluginPortal()
}

dependencyResolutionManagement {
    versionCatalogs.create("libs") {
        from(files("../gradle/libs.versions.toml"))
    }
}

rootProject.name = "build-settings"
