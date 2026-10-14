## TestBalloon browser integration via Playwright

TestBalloon supports the new browser DSL for Kotlin 2.4.20 and above. This directory contains an example.

### Preparation

Before using this example, run the following Gradle tasks at the `testBalloon` project root:

* `./gradlew publishAllPublicationsToIntegrationTestRepository`
* `./gradlew -p experiments/js-browser-playwright-integration kotlinUpgradePackageLock kotlinWasmUpgradePackageLock`

Repeat after a change to the root build.

### Remaining issues

* [KT-90261](https://youtrack.jetbrains.com/issue/KT-90261) KGP/JS/Playwright: TestBalloon cannot configure browser block
* [KT-89269](https://youtrack.jetbrains.com/issue/KT-89269) KGP/JS/Playwright: "Failed to load Kotlin WASM JS tests entry point TypeError"

### Running tests

Selecting tests via Gradle CLI arguments (run from the `testBalloon` project root):

* `./gradlew -p experiments/js-browser-playwright-integration "cleanJsBrowserTest" "jsBrowserTest" --tests "com.example.NestingCommon↘middle↘middle 1*" --tests "com.example.NestingCommon↘middle↘this,does not exist*"`

* `./gradlew -p experiments/js-browser-playwright-integration "cleanWasmJsBrowserTest" "wasmJsBrowserTest" --tests "com.example.NestingCommon↘middle↘middle 1*" --tests "com.example.NestingCommon↘middle↘this,does not exist*"`

> Playwright integration for Wasm is currently disabled. See `disablePlaywrightOnJsWasmForKt89269` in `build.gradle.kts`.

Selecting tests via environment variables prioritized over CLI arguments (run from this directory):

* `env TESTBALLOON_INCLUDE_PATTERNS="com.example.NestingCommon↘middle↘middle 1*" TESTBALLOON_EXCLUDE_PATTERNS="com.example.NestingCommon↘middle↘this,does not exist*" ./gradlew "cleanJsBrowserTest" "jsBrowserTest" --tests "com.example.NestingCommon↘middle↘lower*" --info`

* `env TESTBALLOON_INCLUDE_PATTERNS="com.example.NestingCommon↘middle↘middle 1*" TESTBALLOON_EXCLUDE_PATTERNS="com.example.NestingCommon↘middle↘this,does not exist*" ./gradlew "cleanWasmJsBrowserTest" "wasmJsBrowserTest" --tests "com.example.NestingCommon↘middle↘lower*" --info`
