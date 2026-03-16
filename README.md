![TestBalloon logo](documentation/images/Logo_Text_291x74-light.png#gh-light-mode-only)
![TestBalloon logo](documentation/images/Logo_Text_291x74-dark.png#gh-dark-mode-only)

[![Maven Central](https://img.shields.io/maven-central/v/de.infix.testBalloon/testBalloon-framework-core)](https://central.sonatype.com/artifact/de.infix.testBalloon/testBalloon-framework-core)
[![IntelliJ IDEA plugin](https://img.shields.io/jetbrains/plugin/v/27749?label=IntelliJ%20IDEA%20plugin)](https://plugins.jetbrains.com/plugin/27749-testballoon)
[![Slack channel](https://img.shields.io/static/v1?logo=slack&color=green&label=kotlinlang&message=%23testballoon)](https://kotlinlang.slack.com/archives/C09FQGG85EC)
[![GitHub License](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)

TestBalloon is a DSL-based Kotlin-first test framework. Powered by a compiler plugin, TestBalloon lets you configure tests with plain Kotlin instead of restrictive, annotation-based magic. TestBalloon's concise API is extremely flexible and easy to customize for any test setup.

TestBalloon is compatible with existing assertion libraries and Kotlin Power Assert. It supports all Kotlin target platforms in first-party quality (including Android device tests and Robolectric). TestBalloon has full coroutine support built in and can run your tests with the highest degree of parallelism available.

## Characteristics

✅️ **State-of-the-art capabilities**: Parameterized tests, multi-level hierarchy, coroutines, coroutine context inheritance, deep parallelism, fixtures, expressive names, scope-friendly DSL, decorator chains for configuration.

✅️ **Deep native integration** with the platforms' existing APIs and build tooling, using the familiar Gradle tasks and Kotlin's own platform-specific test runtimes.

✅️ Full support for **all Kotlin target platforms**: JVM, JS, WebAssembly, Native (Linux, Windows, iOS, macOS and other Apple targets), Android host-side tests, Android device-side tests, Robolectric.

For details and quick start information, please visit [TestBalloon's documentation](https://infix-de.github.io/testBalloon/).

To find out more about why to use TestBalloon, [look here](https://infix-de.github.io/testBalloon/latest/overview/why/).

See also these [tips on how to test effectively](https://infix-de.github.io/testBalloon/latest/how-to/effective-testing/).

### Kotlin Multiplatform

![TestBalloon example test run – Kotlin Multiplatform](documentation/website/docs/overview/assets/example-test-run-kmp-light.png#gh-light-mode-only)
![TestBalloon example test run – Kotlin Multiplatform](documentation/website/docs/overview/assets/example-test-run-kmp-dark.png#gh-dark-mode-only)

### Android device-side test

<details>
  <summary>Android device-side test example</summary>

![TestBalloon example test run – Android Device](documentation/website/docs/overview/assets/example-test-run-android-device-light.png#gh-light-mode-only)
![TestBalloon example test run – Android Device](documentation/website/docs/overview/assets/example-test-run-android-device-dark.png#gh-dark-mode-only)
</details>

### Android host-side test

<details>
  <summary>Android host-side test example</summary>

![TestBalloon example test run – Android Host](documentation/website/docs/overview/assets/example-test-run-android-host-light.png#gh-light-mode-only)
![TestBalloon example test run – Android Host](documentation/website/docs/overview/assets/example-test-run-android-host-dark.png#gh-dark-mode-only)
</details>

## Documentation

Please visit [TestBalloon's documentation](https://infix-de.github.io/testBalloon/) with

* [quick start information](https://infix-de.github.io/testBalloon/latest/getting-started/first-steps/),
* a [how-to guide](https://infix-de.github.io/testBalloon/latest/how-to/effective-testing/),
* a [complete API reference](https://infix-de.github.io/testBalloon/latest/api/),
* and more.
