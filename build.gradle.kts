plugins {
    id("buildLogic.common").apply(false)
    id("buildLogic.dokka")
    alias(libs.plugins.de.infix.gradle.plugins.kotlin.multiplatform.js)
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
        }
    }

    register("integrationTests") {
        group = "verification"

        dependsOn(":integration-test:test")
    }
}
