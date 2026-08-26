plugins {
    id("buildLogic.common").apply(false)
    id("buildLogic.dokka")
}

tasks {
    for ((targetSubSet, kmpTaskName) in mapOf("AllTargets" to "allTests", "JvmOnly" to "jvmTest")) {
        register("componentTests$targetSubSet") {
            group = "verification"
            description = "Run tests for all releasable TestBalloon components."

            dependsOn(gradle.includedBuild("testBalloon-compiler-plugin").task(":test"))
            dependsOn(gradle.includedBuild("testBalloon-gradle-plugin").task(":test"))
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
}
