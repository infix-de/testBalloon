import buildLogic.PushReleaseSetTags
import buildLogic.TagReleaseSet

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
            dependsOn(":testBalloon-integration-robolectric:testAndroidHostTest")
        }
    }

    register("integrationTests") {
        group = "verification"

        dependsOn(":integration-test:test")
    }

    register<TagReleaseSet>("tagReleaseSet") {
        description = "Tags all the versions belonging to a release set"
        group = "release"
    }

    register<PushReleaseSetTags>("pushReleaseSetTags") {
        description = "Pushes tags for all versions belonging to a release set, except for the last regular release"
        group = "release"
    }
}
