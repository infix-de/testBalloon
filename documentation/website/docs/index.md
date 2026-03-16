# ![TestBalloon logo](assets/Logo_Text_291x74-light.png#only-light)![TestBalloon logo](assets/Logo_Text_291x74-dark.png#only-dark)

TestBalloon is a DSL-based Kotlin-first test framework. Powered by a compiler plugin, TestBalloon lets you configure tests with plain Kotlin instead of restrictive, annotation-based magic. TestBalloon's concise API is extremely flexible and easy to customize for any test setup.

TestBalloon is compatible with existing assertion libraries and Kotlin Power Assert. It supports all Kotlin target platforms in first-party quality (including Android device tests and Robolectric). TestBalloon has full coroutine support built in and can run your tests with the highest degree of parallelism available.

## Characteristics

<div class="annotate" markdown>

- [x] **State-of-the-art capabilities**: Parameterized tests, multi-level hierarchy, coroutines, coroutine context inheritance, deep parallelism, fixtures, expressive names, scope-friendly DSL, decorator chains for configuration.
- [x] **Deep native integration** with the platforms' existing APIs and build tooling, using the familiar Gradle tasks and Kotlin's own platform-specific test runtimes.
- [x] Support for **all Kotlin target platforms**: JVM, JS(1), WebAssembly(2), Native(3), Android host-side tests, Android device-side tests, Robolectric.

</div>

1. JavaScript support includes JS/Node and JS/browser.
2. WebAssembly support includes Wasm/JS and Wasm/WASI.
3. Native support includes Linux, Windows, iOS, macOS and other Apple targets.

=== "Kotlin Multiplatform"

    ![TestBalloon example test run – Kotlin Multiplatform](overview/assets/example-test-run-kmp-light.png#only-light)
    ![TestBalloon example test run – Kotlin Multiplatform](overview/assets/example-test-run-kmp-dark.png#only-dark)

=== "Android Device"

    ![TestBalloon example test run – Android Device](overview/assets/example-test-run-android-device-light.png#only-light)
    ![TestBalloon example test run – Android Device](overview/assets/example-test-run-android-device-dark.png#only-dark)

=== "Android Host"

    ![TestBalloon example test run – Android Host](overview/assets/example-test-run-android-host-light.png#only-light)
    ![TestBalloon example test run – Android Host](overview/assets/example-test-run-android-host-dark.png#only-dark)

To find out more about why to use TestBalloon, [look here](overview/why.md).

## Documentation Overview

This documentation is organized as follows:

* [Getting started](getting-started/first-steps.md) introduces TestBalloon's capabilities one by one.
* The [How-to guide](how-to/effective-testing.md) contains guidance about effective testing and migrating to TestBalloon.
* The [API Reference](api/index.html) provides comprehensive documentation for TestBalloon's public API.

In addition, there are [Support](support.md) touchpoints and a [Blog](blog/index.md) covering TestBalloon developments.
