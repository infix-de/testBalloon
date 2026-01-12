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
    google {
        @Suppress("UnstableApiUsage")
        mavenContent {
            includeGroupAndSubgroups("androidx")
            includeGroupAndSubgroups("com.android")
            includeGroupAndSubgroups("com.google")
        }
    }
    mavenCentral()
    maven(uri("""{{path:integration-test-repository}}"""))
    System.getProperty("user.home")?.let { home ->
        maven(url = uri("$home/.m2/local-repository"))
    }
    maven("https://redirector.kotlinlang.org/maven/dev")
    maven("https://packages.jetbrains.team/maven/p/kt/dev/org/jetbrains/kotlin/kotlin-compiler/")
    // Note: The 'dev' repo is unstable, releases are deleted after (two?) weeks.
    // The stable Kotlin pre-release repo is: https://packages.jetbrains.team/maven/p/kt/bootstrap
    gradlePluginPortal()
}
