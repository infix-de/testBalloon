## TestBalloon test filtering for browsers (Karma vs. Playwright)

With TestBalloon, test filters can contain arbitrary Unicode characters and have a special format (a multi-level-hierarchy path using `↘` as the path element separator):

E.g., to select [this test](https://github.com/infix-de/testBalloon/blob/2968f8f8a239890adc677459240a8bf8811c9ce4/examples/general/src/commonTest/kotlin/com/example/NestingTestSuites.kt#L19-L21), you'd use a Gradle invocation like this:  `./gradlew ":examples:general:cleanJsBrowserTest" ":examples:general:jsBrowserTest" --tests "com.example.NestingCommon↘integer operations/stuff↘max*"`

> _NOTE: TestBalloon has its own internal test "runner", and evaluates the filter patterns directly. It does not use Mocha to filter tests._

### Using the Gradle filter patterns

TestBalloon uses the raw filter values which Gradle supplies via the following attributes of `(KotlinJsTest.filter as DefaultTestFilter)`:

* `commandLineIncludePatterns`
* `includePatterns`
* `excludePatterns`

In the above example, the value of `commandLineIncludePatterns` would be `setOf("com.example.NestingCommon↘integer operations/stuff↘max*")`. (In addition, a user could set `includePatterns` and `excludePatterns` in the Gradle build script.)

### How it is implemented with Karma

With Karma, TestBalloon's workflow is like this:

1. It delays evaluating the filter values via `gradle.taskGraph.whenReady { ... }` to make sure the `filter` values are in fact available ([source](https://github.com/infix-de/testBalloon/blob/975c681fc74db96f5e246fff9ae442c545b60be5/testBalloon-gradle-plugin/layer/kotlin-2-2-0/src/main/kotlin/de/infix/testBalloon/gradlePlugin/internal/TestBalloonGradlePluginBase.kt#L222)).
2. It then invokes `configureKarma()` ([source](https://github.com/infix-de/testBalloon/blob/975c681fc74db96f5e246fff9ae442c545b60be5/testBalloon-gradle-plugin/layer/kotlin-2-2-0/src/main/kotlin/de/infix/testBalloon/gradlePlugin/internal/TestBalloonGradlePluginBase.kt#L267)).
3. `configureKarma()` obtains the Gradle `filter` values as `secondaryIncludePatterns` and `secondaryExcludePatterns` ([source](https://github.com/infix-de/testBalloon/blob/d2b6decaebe6ac3f2452707f75a9ca2514a76027/testBalloon-gradle-plugin/layer/kotlin-2-2-0/src/main/kotlin/de/infix/testBalloon/gradlePlugin/internal/layer/kotlin220/TestBalloonGradlePlugin_2_2_0.kt#L84-L87)). (Why secondary? Because in TestBalloon, environment variables have priority, so these supply the `primary` patterns. We can ignore this implementation detail.)
4. `configureKarma()` invokes `testBalloonEnvironment()` to retrieve a map of simulated environment variables used to configure the TestBalloon "runner" ([source](https://github.com/infix-de/testBalloon/blob/394cff2035529480cec51280cf9667c62d36eee8/testBalloon-gradle-plugin/layer/kotlin-2-2-0/src/main/kotlin/de/infix/testBalloon/gradlePlugin/internal/Project.kt#L55-L79)).
5. `configureKarma()` creates a Karma configuration which supplies the (simulated) environment variables to the browser client ([source](https://github.com/infix-de/testBalloon/blob/d2b6decaebe6ac3f2452707f75a9ca2514a76027/testBalloon-gradle-plugin/layer/kotlin-2-2-0/src/main/kotlin/de/infix/testBalloon/gradlePlugin/internal/layer/kotlin220/TestBalloonGradlePlugin_2_2_0.kt#L114-L131)).
6. Since TestBalloon has its own integrated runner and uses its own test filtering mechanism, it explicitly disables any filtering values, preventing them from reaching Mocha ([source](https://github.com/infix-de/testBalloon/blob/975c681fc74db96f5e246fff9ae442c545b60be5/testBalloon-gradle-plugin/layer/kotlin-2-2-0/src/main/kotlin/de/infix/testBalloon/gradlePlugin/internal/TestBalloonGradlePluginBase.kt#L274-L284)).
7. On the browser side, TestBalloon evaluates the simulated environment variables ([source](https://github.com/infix-de/testBalloon/blob/97d7f4020e491b63280fd3d87d90a7963a19c1f5/testBalloon-gradle-plugin/layer/kotlin-2-4-20/src/main/kotlin/de/infix/testBalloon/gradlePlugin/internal/layer/kotlin2420/TestBalloonGradlePlugin_2_4_20.kt#L60-L64)) at startup and configures its "runner" accordingly.

### How it is implemented with Playwright

TestBalloon supports the new browser DSL for Kotlin 2.4.20 and above. This directory contains an example.

The workflow is:

1. TestBalloon sets up an implementation of `KotlinJsTestsLocation` ([source](https://github.com/infix-de/testBalloon/blob/97d7f4020e491b63280fd3d87d90a7963a19c1f5/testBalloon-gradle-plugin/layer/kotlin-2-4-20/src/main/kotlin/de/infix/testBalloon/gradlePlugin/internal/layer/kotlin2420/TestBalloonGradlePlugin_2_4_20.kt#L46-L88)).
2. Its `url` provider supplies the "runner" configuration to TestBalloon via URL parameters ([source](https://github.com/infix-de/testBalloon/blob/97d7f4020e491b63280fd3d87d90a7963a19c1f5/testBalloon-gradle-plugin/layer/kotlin-2-4-20/src/main/kotlin/de/infix/testBalloon/gradlePlugin/internal/layer/kotlin2420/TestBalloonGradlePlugin_2_4_20.kt#L78-L87)), **but currently has no access to the Gradle `filter` values** ([source](https://github.com/infix-de/testBalloon/blob/97d7f4020e491b63280fd3d87d90a7963a19c1f5/testBalloon-gradle-plugin/layer/kotlin-2-4-20/src/main/kotlin/de/infix/testBalloon/gradlePlugin/internal/layer/kotlin2420/TestBalloonGradlePlugin_2_4_20.kt#L60-L64)).
3. On the browser side, TestBalloon evaluates the URL parameters ([source](https://github.com/infix-de/testBalloon/blob/97d7f4020e491b63280fd3d87d90a7963a19c1f5/testBalloon-gradle-plugin/layer/kotlin-2-4-20/src/main/kotlin/de/infix/testBalloon/gradlePlugin/internal/layer/kotlin2420/TestBalloonGradlePlugin_2_4_20.kt#L60-L64)) at startup and configures its "runner" accordingly.

> _NOTE: Mocha is not used at all in this setup. TestBalloon provides results via its own TeamCity reporting._

#### The problem

Test filtering via Gradle's `--tests` option (or buildscript filters) does not work with the new browser DSL, because TestBalloon has no access to the filter values.

#### Suggested solution

* In `createTestExecutionSpec()` ([source](https://github.com/JetBrains/kotlin/blob/68fffe36d5c3b1693c12ea186f89d40a0c9c9104/libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/js/testing/playwright/KotlinPlaywrightJsTestFramework.kt#L132-L135)), KGP uses the test filter patterns to create Mocha's `cliArgs`, which it then passes to `createPwRunnerSpec()` ([source](https://github.com/JetBrains/kotlin/blob/68fffe36d5c3b1693c12ea186f89d40a0c9c9104/libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/js/testing/playwright/KotlinPlaywrightJsTestFramework.kt#L162-L178)).
* At that point, it should also pass the raw values of `task.includePatterns` and `task.excludePatterns`.
* Then, `BrowserRunnerInput.createPwRunnerSpec` could pass these on to the point where the url is constructed by a custom implementation of `KotlinJsTestsLocation`.
    * For example, `KotlinJsTestsLocation.url` could be changed from `val url: Provider<URI>` to `fun url(includePatterns: Set<String>, excludePatterns: Set<String>): URI`, so that the filter patterns can be passed as custom URL parameters.
    * Alternative: `KotlinJsTestsLocation` could have two properties like `var includePatterns: Set<String>` and `var excludePatterns: Set<String>`, which would be populated by `BrowserRunnerInput.createPwRunnerSpec` before the `url` provider is read.

### Other

#### Run tests

The following invocation will (wrongly) run all tests, because the `--tests` parameter is not passed to TestBalloon:

* `./gradlew ":experiments:cleanJsBrowserTest" ":experiments:jsBrowserTest" --tests "com.example.NestingCommon↘middle↘middle 1*"`

The following invocation will run the selected test, because the filtering occurs via environment variables, which TestBalloon recognizes:

* `env TESTBALLOON_INCLUDE_PATTERNS="com.example.NestingCommon↘middle↘middle 1*" ./gradlew ":experiments:cleanJsBrowserTest" ":experiments:jsBrowserTest" --tests "com.example.NestingCommon↘middle↘middle 1*"`
