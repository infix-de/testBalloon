The TestBalloon repository contains examples covering typical use cases:

* [TestBalloon’s capabilities in general](https://github.com/infix-de/testBalloon/tree/main/examples/general), including
    * parameterized (dynamic, data-driven) tests,
    * fixtures (shared context),
    * custom test variants,
    * custom reports,
    * concurrency,
    * other configurations available via `TestConfig`.
* [Using TestBalloon for an **Android app**](https://github.com/infix-de/testBalloon/tree/main/examples/android/build.gradle.kts), containing
    * host-side tests (a.k.a. unit tests),
    * device-side tests (a.k.a. instrumented tests),
    * using JUnit 4 rules, and
    * a [**Jetpack Compose** UI test](https://github.com/infix-de/testBalloon/tree/main/examples/android/src/androidTest/kotlin/com/example/ComposeTestsWithTestBalloon.kt).
* [Using TestBalloon for a **Kotlin Multiplatform _plus_ Android library**](https://github.com/infix-de/testBalloon/tree/main/examples/multiplatform-library-with-android), containing
    * host-side tests (a.k.a. unit tests),
    * device-side tests (a.k.a. instrumented tests).
* [Using TestBalloon with **Kotest assertions**](https://github.com/infix-de/testBalloon/tree/main/examples/with-kotest-assertions).
