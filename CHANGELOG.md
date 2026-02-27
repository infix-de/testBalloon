## 0.8.4 (Unreleased)

### Breaking (Robolectric integration only)

* Correct the spelling of `Robolectric` (drop the extra `e`)

Migration required:

* In the IDE, use _Replace in Files_: _Search_ `roboelectric` _Replace_ `robolectric`, enable _Preserve case_.

## 0.8.3 (February 24, 2026)

TestBalloon release variants:

| TestBalloon version | Supported Kotlin versions |
|---------------------|---------------------------|
| 0.8.3-K2.3.20-RC    | 2.3.20-RC                 |
| 0.8.3-K2.3.0        | 2.3.0 … 2.3.10            |
| 0.8.3-K2.2.21       | 2.2.21                    |
| 0.8.3-K2.2.0        | 2.2.0 … 2.2.20            |
| 0.8.3-K2.1.20       | 2.1.20 … 2.1.21           |
| 0.8.3-K2.1.0        | 2.1.0 … 2.1.10            |
| 0.8.3-K2.0.0        | 2.0.0 … 2.0.21            |

### Highlights

* A new [Robolectric integration](https://infix-de.github.io/testBalloon/0.8.3/integrations/roboelectric/) for fast, reliable and configurable Android testing (#65).
* A [new Blog](https://infix-de.github.io/testBalloon/latest/blog/) was born (absorbing the Articles section).

### Other Changes

* Documentation: Added a section for integrations
* TestConfig, TestElement: Added typed parameters identified by keys
* Gradle plugin: Now propagates a custom environment to Apple simulators
* Gradle plugin: New 'testBalloonDiagnostics' task (in the 'help' group) (#63)
* TestBalloon add-ons can now use FrameworkTestUtilities for expanded testing 

## 0.8.2 (January 27, 2026)

TestBalloon release variants:

| TestBalloon version  | Supported Kotlin versions |
|----------------------|---------------------------|
| 0.8.2-K2.3.20-Beta1  | 2.3.20-Beta1 … 2.3.20-RC  |
| 0.8.2-K2.3.0         | 2.3.0 … 2.3.10            |
| 0.8.2-K2.2.21        | 2.2.21                    |
| 0.8.2-K2.2.0         | 2.2.0 … 2.2.20            |
| 0.8.2-K2.1.20        | 2.1.20 … 2.1.21           |
| 0.8.2-K2.1.0         | 2.1.0 … 2.1.10            |
| 0.8.2-K2.0.0         | 2.0.0 … 2.0.21            |

### Fixes

* Restored missing `iosArm64` target. (#60)

## 0.8.1 (January 23, 2026)

TestBalloon release variants:

| TestBalloon version  | Supported Kotlin versions |
|----------------------|---------------------------|
| 0.8.1-K2.3.20-Beta1  | 2.3.20-Beta1              |
| 0.8.1-K2.3.0         | 2.3.0                     |
| 0.8.1-K2.2.21        | 2.2.21                    |
| 0.8.0-K2.2.0         | 2.2.0 … 2.2.20            |

_This AGP 9 update specifically targets those Kotlin versions which are safe to use with AGP 9. For earlier Kotlin versions, please continue using the TestBalloon 0.8.0 variants._

### Highlights

* Android Gradle plugin 9.0.0 support

## 0.8.0 (January 21, 2026)

TestBalloon release variants:

| TestBalloon version  | Supported Kotlin versions |
|----------------------|---------------------------|
| 0.8.0-K2.3.20-Beta1  | 2.3.20-Beta1              |
| 0.8.0-K2.3.0         | 2.3.0                     |
| 0.8.0-K2.2.21        | 2.2.21                    |
| 0.8.0-K2.2.0         | 2.2.0 … 2.2.20            |
| 0.8.0-K2.1.20        | 2.1.20 … 2.1.21           |
| 0.8.0-K2.1.0         | 2.1.0 … 2.1.10            |
| 0.8.0-K2.0.0         | 2.0.0 … 2.0.21            |

### Highlights

* **Test-level fixtures**, providing fresh state per test, are now supported in addition to suite-level fixtures.
* **Fixtures** can now provide their value **as a context or a parameter** to tests. See the [fixture documentation](https://infix-de.github.io/testBalloon/0.8.0-Beta1/getting-started/fixtures/) for details.
* **Android** integration was improved, tests can now use multiple JUnit 4 rules.
* The **API surface** was streamlined with fewer top-level declarations and scoped naming.
* **Migration documentation** is provided for test setups using **JUnit 4/5/6** and **kotlin.test**. The section on **Kotest** migration was also updated.

### Upgrading

Please see the [instructions for upgrading from TestBalloon 0.7.x](https://infix-de.github.io/testBalloon/0.8.0/how-to/upgrading/).

#### Note for Kotlin 2.0.0

Kotlin 2.0.0 projects using JS or Wasm/JS targets should add the following setting to `gradle.properties`:
`kotlin.incremental.js.klib=false`. This avoids incremental compilation errors. Later Kotlin versions are fine.

### Breaking Changes

Former deprecations were removed:

* Class declarations for top-level test suites,
* `dispatcherWithParallelism()`,
* `aroundAll` inside a test suite (superseded by `aroundAll` as part of the `testConfig` parameter),
* `testConfig` assignments inside a test suite (superseded by the `testConfig` parameter),
* `TEST_INCLUDE` environment variable (superseded by `TESTBALLOON_INCLUDE_PATTERNS`).

### Other Changes

#### Android

* Android host-side tests (a.k.a. unit tests) now correctly use JUnit 4 instead of JUnit platform.
* Concurrent/parallel test invocations are now ignored for Android tests, as this apparently conflicts with the Android test infrastructure's handling of Activities, leading to delays and potential hangups (concurrency/parallelism remains available inside tests, of course).
* Examples using the deprecated combination of `com.android.application` and `org.jetbrains.kotlin.multiplatform` Gradle plugins were removed.

#### JVM

* The incubating Gradle JVM Test Suite Plugin is supported by setting a Gradle property containing the Gradle test suite names as a regular expression.
    * Example: `testBalloon.gradleTestSuiteNamesRegex=integrationTest|anotherGradleSuite`.

#### General

* Together with IntelliJ plugin version 0.5.0, the actions "Run", "Debug" and "Jump to source" work from the test run window's results tree view (except for Android device tests). In addition, test run gutter icons display the test status for JVM targets, except Android device test runs.
    * This feature requires a new report format, which is less readable if the new IntelliJ plugin is not installed. In the latter case, setting the Gradle property `testBalloon.reportingMode=intellij-legacy` restores the previous human-readable output format.
* Running individual tests from gutter icons and cross-framework test filtering is now supported with multiple test frameworks in the same module for
    * Android host-side tests (coexistence with other JUnit 4 runners),
    * JVM tests (coexistence with other JUnit Platform-based frameworks like JUnit Jupiter).
* The consistency of file-based reports across test platforms was improved.
* Configuring concurrent invocation now disables `TestScope`, avoiding possible hangups due to thread starvation. (#49)

### Deprecations

* `dispatcherWithParallelism()` – No compelling use case in testing. Scheduled for removal in TestBalloon 0.8.
* `aroundAll` – Use `testConfig = TestConfig.aroundAll { ... }` instead. Scheduled for removal in TestBalloon 0.8.

### Fixes

* (JVM-only projects) Incremental compilation will no longer miss tests. (#47)
* (Android) Display name length limiting in test reports now works correctly. (#44)
* (Android) The total test count is now correctly displayed, avoiding a false positive failure report.
* Running individual tests is now supported on Apple simulator targets. (#53)
* Potentially unsafe characters are now escaped in file-based reporting. (#42)
* Empty or blank test names are now reported with a more helpful message. (#51)

## TestBalloon 0.7.1 (November 5, 2025, updated for K2.3.0 on December 17, 2025)

TestBalloon release variants:

| TestBalloon version | Supported Kotlin versions |
|---------------------|---------------------------|
| 0.7.1-K2.3.0        | 2.3.0                     |
| 0.7.1-K2.2.21       | 2.2.21 … 2.3.0-Beta2      |
| 0.7.1-K2.2.0        | 2.2.0 … 2.2.20            |
| 0.7.1-K2.1.20       | 2.1.20 … 2.1.21           |

### Changes

* This release lowers the **Gradle minimum version requirement** from 9.0 to **Gradle 8.2.1**.

## TestBalloon 0.7.0 (November 4, 2025)

TestBalloon release variants:

| TestBalloon version | Supported Kotlin versions |
|---------------------|---------------------------|
| 0.7.0-K2.2.21       | 2.2.21 … 2.3.0-Beta2      |
| 0.7.0-K2.2.0        | 2.2.0 … 2.2.20            |
| 0.7.0-K2.1.20       | 2.1.20 … 2.1.21           |
| 0.7.0-K2.1.0        | 2.1.0 … 2.1.10            |
| 0.7.0-K2.0.0        | 2.0.0 … 2.0.21            |

### Highlights

* A lot of effort has been invested into making this release even more robust: TestBalloon has mastered **intensive internal edge-case testing** and **real-world test sets of several hundred thousand up to almost 2 million tests**.

* An **extensive documentation site** is available at https://infix-de.github.io/testBalloon/ along with guidance on [effective testing](https://infix-de.github.io/testBalloon/0.7.0/how-to/effective-testing/).

* The **API** has undergone some scrutiny to use more precise language and make everything **as intuitive as possible**. As a result, there is some migration required.

Enjoy!

### Breaking Changes

TestBalloon release variants from 0.7.0-K2.1.20 to 0.7.0-K2.2.21 require Gradle 9. For Gradle 8, please use the corresponding TestBalloon version 0.7.1 variant.

#### New IDE plugin required

If you have installed the TestBalloon IntelliJ IDEA plugin, please upgrade it to version 0.3.0 or higher before using TestBalloon 0.7.0.

#### General migration

Most users can migrate in one to three steps:

1. Replace `import de.infix.testBalloon.framework` with `import de.infix.testBalloon.framework.core`.
2. Replace `TestDiscoverable` with `TestRegistering`.
3. If you have used `AbstractTest*` symbols or annotations (except `TestBalloonExperimentalApi`), change their import package to `de.infix.testBalloon.framework.shared`.

#### Details

**Artifact changes:**

* `testBalloon-framework-abstractions` moved to `testBalloon-framework-shared`

**API changes:**

* Packages:
    * In `testBalloon-framework-shared`: `de.infix.testBalloon.framework` moved to `import de.infix.testBalloon.framework.shared`
    * In `testBalloon-framework-core`: `de.infix.testBalloon.framework` moved to `import de.infix.testBalloon.framework.core`

* `@TestDiscoverable`: renamed to `@TestRegistering`

* `AbstractTestElement`, `TestElement`:
    * `testElementPath`: type changed to `Path`
    * `testElementIsEnabled`: changed to `val`
    * `testElementParent`, `testElementName`, `testElementDisplayName`: removed from the public API

* `AbstractTestSuite`, `TestSuite`:
    * `testElementChildren`: removed from the public API

* `TestConfig`:
    * `report()`: renamed to `executionReport()`

* `TestCoroutineScope`: renamed to `TestExecutionScope`
    * `jUnit4Description`: new extension property (Android only, experimental)
    * `testTimeout`: new experimental property to pass the timeout set by TestConfig.testScope() to Compose UI test invocations

* `TestSuite`
    * `test()`: new optional parameter `displayName`
    * `testWithJUnit4Rule()`: new extension method (Android only, experimental)

* `TestCompartment`
    * `UI`: renamed to `MainDispatcher`

### Deprecations

* Using a class to register a top-level test suite is now deprecated. Scheduled for removal in 0.8.
* The environment variable `TEST_INCLUDE` is now deprecated. Use `TESTBALLOON_INCLUDE_PATTERNS` instead.
* Using a `testConfig` assignment inside a test suite is deprecated. Use `testSuite(..., testConfig = ...)` instead. Scheduled for removal in 0.8.

### Notable changes

#### Test reporting

Test reporting (XML, HTML) now operates in two reporting modes, tailored for

- (a) best display (and speed) in IntelliJ, or
- (b) safe file name lengths for file-based reports.

The normal operation is to choose one or the other, depending on whether tests run in IntelliJ or not. This can be customized via Gradle properties:

- `testBalloon.reportingMode` – `auto` (default), `intellij`, `files`
- `testBalloon.reportsEnabled` – `auto` (default), `true`, `false`

#### Conditional tag-based testing

Custom environment variables can now control test runs on all targets. This also enables [conditional tag-based test selection](https://infix-de.github.io/testBalloon/0.7.0/how-to/effective-testing/#conditional-tag-based-testing).

Environment variables will also be propagated to browsers [if they are declared browser-safe](https://infix-de.github.io/testBalloon/0.7.0/getting-started/integration/#browser-environment-variables).

#### Other

* Test elements with formerly identical names will now be made unique up to a limit of 999_999 instead of just 999.
* Test suites without children now trigger an error. `TestConfig.addPermits(TestPermit.SUITE_WITHOUT_CHILDREN)` suppresses the error.
* Test element wrappers which do not invoke their inner element action now trigger an error. `TestConfig.addPermits(TestPermit.WRAPPER_WITHOUT_INNER_INVOCATION)` suppresses the error.
* Test filtering now _removes_ test elements from the hierarchy instead of _disabling_ them. This greatly speeds up running selected tests from very large test sets, as the reporting infrastructure (IntelliJ, Gradle) previously slowed things down.
* The `testTimeout` property set via [TestConfig.testScope](https://infix-de.github.io/testBalloon/0.7.0/api/testBalloon-framework-core/de.infix.testBalloon.framework.core/test-scope.html)'s `timeout` parameter is now accessible (experimental). This makes it possible to create a [Compose Multiplatform test function](https://infix-de.github.io/testBalloon/0.7.0/how-to/effective-testing/#a-ui-test-with-compose-multiplatform) which conveniently inherits the timeout.

### Fixes

* KSP Gradle configurations are supported, if present. (#34)
* Android Gradle device test configurations depending on `commonTest` are supported, if present. However, due to the AGP/KGP integration being a moving target, issues beyond TestBalloon's control are to be expected.
* Gradle test filtering now works on JS and Wasm/WASI targets. (#28)
* Fix Gradle test reports: "Could not write XML test results". (#35)
* Wasm/JS: Running tests with Kotlin >= 2.1.21-RC and >= 2.2.0-Beta2 no longer produces the error "Identifier 'startUnitTests' has already been declared".
* Android local tests (a.k.a. host-based tests, a.k.a. unit tests) no longer fail if Android Gradle Library Plugin for KMP is used. (#39)

### Other

* API stability control is in place with binary ABI checks, the `@TestBalloonExperimentalApi` annotation, and propagation of `@ExperimentalCoroutinesApi`.

## TestBalloon 0.6.0 (August 19, 2025)

TestBalloon release variants:

| TestBalloon version | Supported Kotlin versions |
|---------------------|---------------------------|
| 0.6.0-K2.2.20-RC    | 2.2.20-RC                 |
| 0.6.0-K2.2.0        | 2.2.0 … 2.2.20            |
| 0.6.0-K2.1.20       | 2.1.20 … 2.1.21           |
| 0.6.0-K2.1.0        | 2.1.0 … 2.1.10            |
| 0.6.0-K2.0.0        | 2.0.0 … 2.0.21            |

### Highlights

* Support for Android instrumented tests and Android local tests. (#31)

## TestBalloon 0.5.0 (August 9, 2025)

TestBalloon release variants:

| TestBalloon version | Supported Kotlin versions |
|---------------------|---------------------------|
| 0.5.0-K2.2.20-Beta2 | 2.2.20-Beta2              |
| 0.5.0-K2.2.0        | 2.2.0 … 2.2.20            |
| 0.5.0-K2.1.20       | 2.1.20 … 2.1.21           |
| 0.5.0-K2.1.0        | 2.1.0 … 2.1.10            |
| 0.5.0-K2.0.0        | 2.0.0 … 2.0.21            |

### Highlights

* XML test reports for all targets (#26)
* Multi-level hierarchy display in IntelliJ IDEA for all targets (#25)
* Full support for Gradle 9.0.0 (#29)

### Fixes

* Concurrent tests can no longer create multiple instances of a Fixture (#30)

## TestBalloon 0.4.0 (July 25, 2025)

TestBalloon release variants:

| TestBalloon version | Supported Kotlin versions |
|---------------------|---------------------------|
| 0.4.0-K2.2.20-Beta1 | 2.2.20-Beta1              |
| 0.4.0-K2.2.0        | 2.2.0 … 2.2.20            |
| 0.4.0-K2.1.20       | 2.1.20 … 2.1.21           |
| 0.4.0-K2.1.0        | 2.1.0 … 2.1.10            |
| 0.4.0-K2.0.0        | 2.0.0 … 2.0.21            |

### Highlights

* Incremental compilation support (#10)
* Improved test configuration error reporting (#15)

### Changes

* Configuration: Prevent a custom test or test suite from registering with the wrong parent (#3)
* Configuration: Inner testSuite invocations now accept a displayName

### Fixes

* Gradle plugin: Fix build failure when TestBallon plugin is specified before KGP (#18)
* Gradle plugin: Fix configuration cache misses (#20)

## TestBalloon 0.3.3-K2.2.0 (June 23, 2025)

### Highlights

* Support Kotlin 2.2.0 (released today) by default

## TestBalloon 0.3.3 (June 18, 2025)

TestBalloon release variants:

| TestBalloon version | Supported Kotlin versions |
|---------------------|---------------------------|
| 0.3.3-K2.2.0-RC     | 2.2.0-RC                  |
| 0.3.3-K2.1.21       | 2.1.20 … 2.1.21           |
| 0.3.3-K2.1.0        | 2.1.0 … 2.1.10            |
| 0.3.3-K2.0.0        | 2.0.0 … 2.0.21            |

### Changes

* Add Kotlin/Native targets, including iOS, macOS (#2)
* Support builds on Windows
* Fix defective 0.3.2 release

## TestBalloon 0.3.2 (May 27, 2025)

**NOTE: Please skip this release.**

Its artifacts were accidentally compiled with Kotlin 2.2.

## TestBalloon 0.3.1 (May 27, 2025)

TestBalloon release variants:

| TestBalloon version | Supported Kotlin versions |
|---------------------|---------------------------|
| 0.3.1-K2.2.0-RC     | 2.2.0-RC                  |
| 0.3.1-K2.1.21       | 2.1.20 … 2.1.21           |
| 0.3.1-K2.1.0        | 2.1.0 … 2.1.10            |
| 0.3.1-K2.0.0        | 2.0.0 … 2.0.21            |

### Changes

* Configure Kotlin/Java compatibility consistently
* Lower JDK requirement from version 17 to version 11

## TestBalloon 0.3.0 (May 23, 2025)

TestBalloon release variants:

| TestBalloon version | Supported Kotlin versions |
|---------------------|---------------------------|
| 0.3.0-K2.2.0-RC     | 2.2.0-RC                  |
| 0.3.0-K2.1.21       | 2.1.20 … 2.1.21           |
| 0.3.0-K2.1.0        | 2.1.0 … 2.1.10            |
| 0.3.0-K2.0.0        | 2.0.0 … 2.0.21            |

### Highlights

Initial public release from KotlinConf 2025. 
