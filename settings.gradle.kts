pluginManagement {
    includeBuild("build-logic")
    includeBuild("build-settings")
    includeBuild("testBalloon-gradle-plugin")
}

plugins {
    id("buildSettings")
}

rootProject.name = "testBalloon"

includeBuild("testBalloon-framework-shared")
include(":testBalloon-framework-core")

includeBuild("testBalloon-compiler-plugin")

include(":integration-test")

include(":documentation:website")
include(":documentation:website:snippets")

includeBuild("documentation/dokka-plugin-internal-api-hiding")
includeBuild("documentation/dokka-plugin-navigation-node-hiding")

include(":testBalloon-integration-kotest-assertions")
include(":testBalloon-integration-blocking-detection")
include(":testBalloon-integration-robolectric")

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
