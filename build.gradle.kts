plugins {
    id("buildLogic.common").apply(false)
    id("buildLogic.dokka")
}

tasks {
    for ((targetSubSet, kmpTaskName) in mapOf("AllTargets" to "allTests", "JvmOnly" to "jvmTest")) {
        register("componentTests$targetSubSet") {
            group = "verification"

            dependsOn(":testBalloon-compiler-plugin:test")
            dependsOn(":testBalloon-gradle-plugin:test")
            dependsOn(":testBalloon-framework-core:$kmpTaskName")

            dependsOn(":testBalloon-integration-kotest-assertions:$kmpTaskName")
            dependsOn(":testBalloon-integration-blocking-detection:$kmpTaskName")

            // With AGP 8.3.2, Android+KMP host tests could not be configured
            // dependsOn(":testBalloon-integration-roboelectric:$kmpTaskName")
        }
    }

    register("integrationTests") {
        group = "verification"

        dependsOn(":integration-test:test")
    }
}
