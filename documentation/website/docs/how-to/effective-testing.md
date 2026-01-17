Using TestBalloon's powers, how can we test better with less effort? This section offers guidance for typical scenarios.

## Expressive names

Make tests communicate their purpose:

```kotlin
--8<-- "EffectiveTesting.kt:expressive-test-names"
```

## Write once, test many

Let immutable state describe and drive your test cases:

```kotlin
--8<-- "TestsAndSuites.kt:ParameterizedTests"
```

## Cover edge cases and samples

Test edge cases and/or random samples with value sources (generators):

```kotlin
--8<-- "TestsAndSuites.kt:TransactionServiceTests-accepted-counts"
```

1. Edge cases.
2. Generating repeatable pseudo-random values with a seed.

One test per sample documents which tests were actually run:

![Generative test results](assets/effective-testing/TransactionServiceTests-accepted-counts-light.png#only-light)
![Generative test results](assets/effective-testing/TransactionServiceTests-accepted-counts-dark.png#only-dark)

## Supply fresh state to multiple tests

To conveniently provide each test with fresh state, available as a context via `this`, use a fixture and provide its value as a context for each test:

```kotlin
--8<-- "EffectiveTesting.kt:multiple-tests-with-fresh-state"
```

1. `signIn` can be a suspending function.
2. If you want to provide multiple values, use an `object`  expression inside the fixture and `asContextForEach`.
3. `signOut` may also suspend.

!!! tip

    In this case, tests are fully isolated from each other, and don't need a `TestScope`. They are ideal candidates for concurrent execution.

## Use shared state across multiple tests

To conveniently share state among tests, use a fixture's value as a shared context for all tests:

```kotlin
--8<-- "EffectiveTesting.kt:multiple-tests-sharing-state"
```

1. We can use mutable state here. This is [green code](../getting-started/tests-and-suites.md#green-code-and-blue-code) which exists exclusively at test execution time, preserving [TestBalloon's golden rule](../getting-started/tests-and-suites.md#testballoons-golden-rule).

!!! tip

    Writing tests that build on each other is easy, because, by default, TestBalloon runs tests in the order they appear in the source. Just make sure that you don't configure concurrent execution for them.

## Make tests run fast

### …if all tests avoid non-local mutable state

If you have a module where all tests only mutate local state(1) and don't need a `TestScope`, you can speed up test execution greatly by running them concurrently. To do so, put this declaration anywhere in your test module:
{ .annotate }

1. Ascertain that tests do not share mutable state among each other and do not access global mutable state.

```kotlin
--8<-- "Configuration.kt:concurrent-test-session"
```

1. For technical reasons, a compartment assignment must be done lazily.

### …if most tests avoid non-local mutable state

1. Configure the module's test session for concurrency:

    ```kotlin
    --8<-- "Configuration.kt:concurrent-test-session"
    ```

    1. For technical reasons, a compartment assignment must be done lazily.

2. Put top-level test suites, whose test's access non-local mutable state or need a `TestScope`, in the predefined `Sequential` compartment:

    ```kotlin
    --8<-- "EffectiveTesting.kt:test-suite-with-sequential-compartment"
    ```

    1. For technical reasons, a compartment assignment must be done lazily.

TestBalloon will now execute tests in the `Sequential` compartment sequentially, and also isolate them from all concurrent tests.

### …if only some tests can run concurrently

Put top-level test suites, whose test's can run concurrently and don't need a `TestScope`, in the predefined `Concurrent` compartment:

```kotlin
--8<-- "EffectiveTesting.kt:test-suite-with-concurrent-compartment"
```

1. For technical reasons, a compartment assignment must be done lazily.

TestBalloon will now execute most tests sequentially (by default), and isolate them from those in the `Concurrent` compartment, where they run concurrently.

## A UI test with Jetpack Compose

TestBalloon does not bundle Compose dependencies, but it does provide a `JUnit4RulesContext` to create test-level fixtures supporting JUnit 4 rules.

With it, you can use Jetpack Compose tests inside TestBalloon via `composeTestRule`, [as shown in the Google documentation](https://developer.android.com/develop/ui/compose/testing):

```kotlin
--8<-- "JetpackComposeTests.kt:testballoon-jetpackCompose"
```

1. Deriving a fixture value from `JUnit4RulesContext` enables support for JUnit 4 rules.
2. Instead of annotations, use the `rule()` function to register a `TestRule`.

See complete code in this [**Jetpack Compose** test example]({{ repo.main_url }}/examples/android/src/androidTest/kotlin/com/example/ComposeTestsWithTestBalloon.kt).

## A UI test with Compose Multiplatform

Compose Multiplatform provides an experimental [`runComposeUiTest()`](https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-test.html) API. To use it with TestBalloon, create a custom DSL function like this:(1)
{ .annotate }

1. Using the Compose Multiplatform test API requires an opt-in directive like `@file:OptIn(ExperimentalTestApi::class)`.

```kotlin
@TestRegistering
fun TestSuiteScope.composeTest(name: String, action: suspend ComposeUiTest.() -> Unit) = test(name) {
    @OptIn(TestBalloonExperimentalApi::class) // required for TestBalloon's testTimeout
    runComposeUiTest(
        runTestContext = coroutineContext.minusKey(CoroutineExceptionHandler.Key),
        testTimeout = testTimeout ?: 60.seconds
    ) {
        action()
    }
}
```

With that, you can use Compose Multiplatform tests inside TestBalloon [as shown in the JetBrains documentation](https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-test.html).(1)
{ .annotate }

1. Using the Compose Multiplatform test API requires an opt-in directive like `@file:OptIn(ExperimentalTestApi::class)`.

```kotlin
val ComposeMultiplatformTests by testSuite {
    composeTest("click") {
        setContent {
            ComposableUnderTest()
        }

        onNodeWithText("Button").performClick()
        onNodeWithText("Success").assertExists()
    }
}
```

## Handling flaky tests

One way of handling flaky tests is to repeat them until they succeed.

Create a `TestConfig` extension:

```kotlin
--8<-- "EffectiveTesting.kt:repeatOnFailure"
```

Use it like this:

```kotlin
--8<-- "EffectiveTesting.kt:FlakyTests"
```

The outcome:

![Flaky test results](assets/effective-testing/FlakyTests-light.png#only-light)
![Flaky test results](assets/effective-testing/FlakyTests-dark.png#only-dark)

## Conditional tag-based testing

TestBalloon provides the option of using environment variables to control test execution on all Kotlin targets.(1)
{ .annotate }

1. JS browsers and Android (emulated or physical) devices do not natively support environment variables. TestBalloon provides a (simulated) environment for those. For Android device-side tests, you need to set them via [instrumentation arguments](../getting-started/integration.md/#android-device-environment-variables). For JS browsers, you need to declare them as [browser-safe](../getting-started/integration.md/#browser-environment-variables).

If you define tags(1) and a `TestConfig` extension like this,
{ .annotate }

1. These are _your_ tags, literally, in plain Kotlin, instead of some complex pre-defined tag regime.

```kotlin
--8<-- "EffectiveTesting.kt:my-tags"
```

…you can use a `TEST_TAGS` environment variable to conditionally run tests and suites at any level of the test element hierarchy:

```kotlin
--8<-- "EffectiveTesting.kt:tag-based-tests"
```

## Use a temporary directory (and keep it on failures)

Some tests require a temporary directory. Let's suppose you want to keep it for inspection if there were test failures – but not on CI. Let's also require the directory name to identify the test by default.

Create a test fixture for the directory, deleting it at the end of its lifecycle (its test or test suite):

```kotlin
--8<-- "EffectiveTestingJvm.kt:temporary-directory-fixture"
```

1. Prefixes the directory with a name identifying the test suite.
2. The directory (a `Path`) is the fixture's value.
3. Show the path on failures for easy inspection. 

If you need a directory per test, use it like this:

```kotlin
--8<-- "EffectiveTestingJvm.kt:temporary-directory-per-test"
```

If you need a directory per test suite, use it like this:(1)
{ .annotate }

1. You can also use the others variants like [`asParameterForAll`](../getting-started/fixtures.md#suite-level-fixtures).

```kotlin
--8<-- "EffectiveTestingJvm.kt:temporary-directory-per-suite"
```
