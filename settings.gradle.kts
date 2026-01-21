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
// include(":documentation:website:snippets") // not supported with Kotlin 2.0.0 and AGP 8.3.2
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
// include(":examples:multiplatform-library-with-android") // not supported with Kotlin 2.0.0 and AGP 8.3.2

// include(":comparisons:using-kotlin-test") // not supported with Kotlin 2.0.0 and AGP 8.3.2
// include(":comparisons:using-testBalloon") // not supported with Kotlin 2.0.0 and AGP 8.3.2

// include(":experiments") // using Android+KMP, not supported with Kotlin 2.0.0 and AGP 8.3.2
