pluginManagement {
    includeBuild("build-logic")
    includeBuild("build-settings")
}

plugins {
    id("buildSettings")
}

rootProject.name = "testBalloon"

include(":testBalloon-framework-shared")
include(":testBalloon-framework-core")
include(":testBalloon-gradle-plugin")
include(":testBalloon-compiler-plugin")

include(":integration-test")

include(":documentation:website")
include(":documentation:website:snippets")
include(":documentation:dokka-plugin-internal-api-hiding")
include(":documentation:dokka-plugin-navigation-node-hiding")

include(":testBalloon-integration-kotest-assertions")
include(":testBalloon-integration-blocking-detection")

include(":examples:general")
include(":examples:with-kotest-assertions")
include(":examples:with-parameterize")
include(":examples:android")
include(":examples:jvm-only")
include(":examples:jvm-with-gradle-test-suites")
include(":examples:multiplatform-library-with-android")

include(":comparisons:using-kotlin-test")
include(":comparisons:using-testBalloon")

include(":experiments")
