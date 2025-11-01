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

include(":testBalloon-integration-kotest-assertions")
include(":testBalloon-integration-blocking-detection")

include(":examples:general")
include(":examples:with-kotest-assertions")
include(":examples:android")
include(":examples:multiplatform-library-with-android")
include(":examples:multiplatform-with-android")

include(":comparisons:using-kotlin-test")
include(":comparisons:using-kotlin-test-multiplatform-with-android")
include(":comparisons:using-testBalloon")

include(":experiments")
