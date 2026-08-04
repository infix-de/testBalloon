import buildLogic.gradleRunCommandLine

plugins {
    id("buildLogic.common").apply(false)
    id("buildLogic.dokka")
}

tasks {
    for ((targetSubSet, kmpTaskName) in mapOf("AllTargets" to "allTests", "JvmOnly" to "jvmTest")) {
        register("componentTests$targetSubSet") {
            group = "verification"
            description = "Run tests for all releasable TestBalloon components."

            dependsOn(":testBalloon-compiler-plugin:test")
            dependsOn(":testBalloon-gradle-plugin:test")
            dependsOn(":testBalloon-framework-core:$kmpTaskName")

            dependsOn(":testBalloon-integration-kotest-assertions:$kmpTaskName")
            dependsOn(":testBalloon-integration-blocking-detection:$kmpTaskName")
            dependsOn(":testBalloon-integration-robolectric:testAndroidHostTest")
        }
    }

    register("integrationTests") {
        group = "verification"
        description = "Run TestBalloon integration tests."

        dependsOn(":integration-test:test")
    }

    register("cleanAll", Exec::class) {
        group = "releasing"
        description = "Runs 'gradlew clean' on all projects."
        workingDir = rootDir
        commandLine = gradleRunCommandLine("--warn", "clean")
    }

    register("deleteKotlinPackageLockFiles", Delete::class) {
        group = "releasing"
        description = "Deletes all Kotlin/JS 'package-lock.json' files."

        delete(
            fileTree(rootDir) {
                include("**/kotlin-js-store/**/package-lock.json")
            }
        )
    }

    register("updateKotlinPackageLockFilesInProjects", Exec::class) {
        group = "releasing"
        description = "Updates Kotlin/JS 'package-lock.json' files in all projects."

        mustRunAfter("deleteKotlinPackageLockFiles")

        workingDir = rootDir
        commandLine = gradleRunCommandLine("--warn", "kotlinUpgradePackageLock", "kotlinWasmUpgradePackageLock")
    }

    register("updateKotlinPackageLockFilesInTemplates", Exec::class) {
        group = "releasing"
        description = "Updates all Kotlin/JS 'package-lock.json' files in all templates."

        mustRunAfter("deleteKotlinPackageLockFiles")
        mustRunAfter("updateKotlinPackageLockFilesInProjects")

        workingDir = rootDir
        environment("PREPARE_PACKAGE_LOCK_FILES_ONLY", "true")
        commandLine = gradleRunCommandLine(":integration-test:test")
    }

    register("updateKotlinPackageLockFiles") {
        group = "releasing"
        description = "Updates all Kotlin/JS 'package-lock.json' files."

        dependsOn("updateKotlinPackageLockFilesInProjects")
        dependsOn("updateKotlinPackageLockFilesInTemplates")
    }

    register("recreateKotlinPackageLockFiles") {
        group = "releasing"
        description = "Recreates fresh Kotlin/JS 'package-lock.json' files."

        dependsOn("deleteKotlinPackageLockFiles")
        dependsOn("updateKotlinPackageLockFiles")
    }
}
