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

include(":documentation:site:docs")
// include(":documentation:site:snippets") // using Android+KMP, not supported with Kotlin 2.0.0 and AGP 8.3.2
include(":documentation:dokka-plugin-hide-internal-api")

include(":testBalloon-integration-kotest-assertions")
include(":testBalloon-integration-blocking-detection")

include(":examples:general")
include(":examples:with-kotest-assertions")
include(":examples:with-parameterize")
include(":examples:android")
// include(":examples:multiplatform-library-with-android") // not supported with Kotlin 2.0.0 and AGP 8.3.2
// include(":examples:multiplatform-with-android") // not supported with Kotlin 2.0.0 and AGP 8.3.2

include(":comparisons:using-kotlin-test")
include(":comparisons:using-kotlin-test-multiplatform-with-android")
include(":comparisons:using-testBalloon")

// include(":experiments") // using Android+KMP, not supported with Kotlin 2.0.0 and AGP 8.3.2
