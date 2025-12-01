TestBalloon is a **next generation Kotlin test framework**, built from the ground up for Kotlin Multiplatform and coroutines.

TestBalloon has a unique combination of characteristics which make it powerful, blazingly fast, _and_ easy to use:

<div class="annotate" markdown>

- [x] **State-of-the-art capabilities**: Parameterized tests, multi-level hierarchy, coroutine context inheritance, deep parallelism, fixtures, expressive names, and a scope-friendly DSL-based API.
- [x] **Deep native integration** with the platforms' existing APIs and build tooling, using the familiar Gradle tasks and Kotlin's own platform-specific test runtimes.
- [x] Support for **all Kotlin target platforms** (JVM, JS, WebAssembly, Native(1), Android local tests, Android device tests).

</div>

1. Native includes Linux, Windows, iOS, macOS and other Apple targets.

=== "Kotlin Multiplatform"

    ![TestBalloon example test run – Kotlin Multiplatform](overview/assets/example-test-run-kmp-light.png#only-light)
    ![TestBalloon example test run – Kotlin Multiplatform](overview/assets/example-test-run-kmp-dark.png#only-dark)

=== "Android Device"

    ![TestBalloon example test run – Android Device](overview/assets/example-test-run-android-device-light.png#only-light)
    ![TestBalloon example test run – Android Device](overview/assets/example-test-run-android-device-dark.png#only-dark)

=== "Android Local"

    ![TestBalloon example test run – Android Local](overview/assets/example-test-run-android-local-light.png#only-light)
    ![TestBalloon example test run – Android Local](overview/assets/example-test-run-android-local-dark.png#only-dark)

To find out more about why to use TestBalloon, [look here](overview/why.md).

## Documentation Overview

This documentation is organized as follows:

* [Getting started](getting-started/first-steps.md) introduces TestBalloon's capabilities one by one.
* The [How-to guide](how-to/effective-testing.md) contains guidance about effective testing and migrating to TestBalloon.
* The [API Reference](api/index.html) provides comprehensive documentation for TestBalloon's public API.

There are [Support](support.md) touchpoints and an incubating selection of [Articles](articles.md) on TestBalloon.
