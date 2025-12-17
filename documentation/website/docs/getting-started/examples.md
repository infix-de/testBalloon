The TestBalloon repository contains examples covering typical use cases:

* [TestBalloon’s capabilities in general]({{ repo.main_url }}/examples/general), including
    * parameterized (dynamic, data-driven) tests,
    * fixtures (shared context),
    * custom test variants,
    * custom reports,
    * concurrency,
    * other configurations available via `TestConfig`.
* [Using TestBalloon for an **Android app**]({{ repo.main_url }}/examples/android/build.gradle.kts), containing
    * host-side tests (a.k.a. unit tests),
    * device-side tests (a.k.a. instrumented tests),
    * using JUnit 4 rules, and
    * a [**Jetpack Compose** UI test]({{ repo.main_url }}/examples/android/src/androidTest/kotlin/com/example/ComposeTestsWithTestBalloon.kt).
* [Using TestBalloon for a **Kotlin Multiplatform _plus_ Android library**]({{ repo.main_url }}/examples/multiplatform-library-with-android), containing
    * host-side tests (a.k.a. unit tests),
    * device-side tests (a.k.a. instrumented tests).
* [Using TestBalloon with **Kotest assertions**]({{ repo.main_url }}/examples/with-kotest-assertions).
