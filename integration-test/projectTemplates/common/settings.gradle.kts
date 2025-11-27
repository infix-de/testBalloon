plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

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
    maven(uri("""{{path:integration-test-repository}}"""))
    google()
    System.getProperty("user.home")?.let { home ->
        maven(url = uri("$home/.m2/local-repository"))
    }
    maven("https://redirector.kotlinlang.org/maven/dev")
    maven("https://packages.jetbrains.team/maven/p/kt/dev/org/jetbrains/kotlin/kotlin-compiler/")
    gradlePluginPortal()
}
