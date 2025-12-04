TestBalloon has a unified API for all Kotlin target platforms, residing in the `common` source set.

!!! info

    TestBalloon integrates thoroughly with the platforms' existing APIs and build tooling, using the familiar Gradle tasks and Kotlin's own platform-specific test runtimes.

TestBalloon supports multi-level nesting of test suites and [deep concurrency](coroutines.md#deep-concurrency-and-parallelism) on all platforms, even where the underlying infrastructure does not.

**Runtime information** and **environment variables** are available on all platforms via the global `testPlatform` variable and its [`TestPlatform`](../api/testBalloon-framework-core/de.infix.testBalloon.framework.core/-test-platform/index.html) interface.

The following sections provide an overview about TestBalloon's integration with its various platforms and build tooling. They are not meant to be exhaustive, but highlight selected details.

## Kotlin Multiplatform

### Gradle

TestBalloon fully integrates with the Kotlin Gradle Plugin ([Multiplatform](https://kotlinlang.org/docs/gradle-configure-project.html#targeting-multiple-platforms) or [JVM](https://kotlinlang.org/docs/gradle-configure-project.html#targeting-the-jvm)). It supports

* the familiar Gradle and Kotlin test tasks,
* Gradle-based test filtering,
* Gradle's test reports (HTML and XML), and
* the Kotlin Gradle Plugin's combined multiplatform test reports.

!!! warning

    Never use the `maxParallelForks` option on Gradle test tasks. Gradle has no idea about the test structure and assumes class-based tests, which TestBalloon does not use.

#### Test selection (filtering)

TestBalloon supports the usual [Gradle test task filtering](https://docs.gradle.org/current/userguide/java_testing.html#test_filtering) options for all Kotlin Multiplatform targets plus Android local (host-based) tests.(1)
{ .annotate }

1. Android device (instrumented) tests do not use Gradle's filtering options since the AGP provides them as _verification_ tasks, not _test_ tasks.

Test selection accepts the pipe `|` character to separate test elements. This is a valid test invocation:

```shell
./gradlew cleanJvmTest jvmTest --tests "com.example.TestSuite|inner suite|*" --no-build-cache --info
```

Alternatively, TestBalloon's own south-east arrow `↘` can be used, or a custom separator if the test patterns begins with one, like `;com.example.TestSuite;inner suite;*`.

!!! warning

    IntelliJ IDEA's run configurations mess with test filtering via `--tests`. In this case, use the `TESTBALLOON_INCLUDE_PATTERNS` environment variable instead, like `TESTBALLOON_INCLUDE_PATTERNS=com.example.TestSuite|inner suite|*`.

To use test selection with **Android device (instrumented) tests**, you have these options:

1. In the IDE's run configuration, use the **instrumentation argument** `TESTBALLOON_INCLUDE_PATTERNS` with the pattern as its value.

2. Pass it via Gradle's command line:

    ```shell
    ./gradlew "-Pandroid.testInstrumentationRunnerArguments.TESTBALLOON_INCLUDE_PATTERNS=com.example.TestSuite|inner suite|*" ...
    ```

3. Use the Android Gradle DSL:

    ```kotlin
    testInstrumentationRunnerArguments["TESTBALLOON_INCLUDE_PATTERNS"] = "com.example.TestSuite|inner suite|*"
    ```

### JVM

TestBalloon registers with Gradle, without requiring any platform-specific configuration. TestBalloon can run alongside other JUnit-based test frameworks in the same module.

TestBalloon supports [deep concurrency](coroutines.md#deep-concurrency-and-parallelism) and tests running in parallel.

### JS, Wasm/JS

TestBalloon fully supports the Kotlin Gradle Plugin's test infrastructure, including test execution via Node.js, or in a browser via Karma.

TestBalloon supports [deep concurrency](coroutines.md#deep-concurrency-and-parallelism) on JS-based platforms, and provides simulated environment variables in browser tests.

#### Environment variables {#browser-environment-variables}

TestBalloon exports only those environment variables into a **browser's simulated environment**, which are declared browser-safe. To do so, use these options (they are cumulative):

1. Set the Gradle property `testBalloon.browserSafeEnvironmentPattern` to a comma-separated list of environment variable names:

    ```properties
    testBalloon.browserSafeEnvironmentPattern=CI|TEST.*
    ```

2. In a build script's `testBalloon` extension, set the parameter `browserSafeEnvironmentPattern`:

    ```kotlin
    testBalloon {
        browserSafeEnvironmentPattern = "CI|TEST.*"
    }
    ``` 

### Native

TestBalloon fully supports the Kotlin Gradle Plugin infrastructure.

TestBalloon supports [deep concurrency](coroutines.md#deep-concurrency-and-parallelism) and tests running in parallel.

## Android

TestBalloon integrates with Android's test infrastructure, the [Android Gradle Plugin (AGP)](https://developer.android.com/build/releases/gradle-plugin), and the [Android Gradle Library Plugin for KMP](https://developer.android.com/kotlin/multiplatform/plugin).

### Local tests

TestBalloon supports Android local (host-based) tests via Android's JUnit 4 runner. Other (non-TestBalloon) JUnit-based tests can execute alongside TestBalloon in the same module.

TestBalloon supports

* JUnit 4 test rules via its `testWithJUnit4Rule()` function,
* [deep concurrency](coroutines.md#deep-concurrency-and-parallelism) and tests running in parallel.

### Device (instrumented) tests

TestBalloon supports Android device tests via Android's JUnit 4 runner. Other (non-TestBalloon) JUnit-based tests can execute alongside TestBalloon in the same module.

TestBalloon supports

* JUnit 4 test rules via its `testWithJUnit4Rule()` function,
* [deep concurrency](coroutines.md#deep-concurrency-and-parallelism) and tests running in parallel (on an emulator or a physical device).

#### Environment variables {#android-device-environment-variables}

For Android device tests, TestBalloon provides simulated environment variables via instrumentation arguments. To set them, you have these options:

1. In the IDE's run configuration, use the **instrumentation argument** with the variable name and value.

2. Pass it via Gradle's command line:

    ```shell
    ./gradlew "-Pandroid.testInstrumentationRunnerArguments.VARIABLE_NAME=VALUE" ...
    ```

3. Use the Android Gradle DSL:

    ```kotlin
    testInstrumentationRunnerArguments["VARIABLE_NAME"] = "VALUE"
    ```

## IntelliJ IDEA

TestBalloon integrates with IntelliJ IDEA. Some of the functionality is provided by the [TestBalloon plugin for IntelliJ IDEA](https://plugins.jetbrains.com/plugin/27749-testballoon).

* Editor windows show run gutter icons to run or debug individual tests or test suites (at any level).
* Test results appear in IntelliJ's test run window, including the results tree display.
* Stack traces in test results hide framework-internal lines by folding.
* Kotlin inspections allow title-case naming for TestBalloon's top-level suite properties.

### Limitations

Currently, TestBalloon's IntelliJ plugin does not support the following:

* Actions _run_, _debug_ and _jump to source_ in the test results tree display.
* Actions _run_, _debug_ and _jump to source_ for failed tests in the inspections window
* The action _rerun failed tests_ in the test run window.
