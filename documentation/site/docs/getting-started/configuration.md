## Introduction

TestBalloon provides two mechanisms to adapt the testing process to your needs via plain Kotlin:

1. `TestSuite` extensions to register custom tests or test suites.

2. `TestConfig`, a uniform builder API to configure test elements at any level:

    === "Single test"

        ![A single test in the test element hierarchy](assets/configuration/test-element-hierarchy-test.svg){ width="600" }

    === "Test suite"

        ![A test suite in the test element hierarchy](assets/configuration/test-element-hierarchy-test-suite.svg){ width="600" }

    === "Test compartment"

        ![A test compartment in the test element hierarchy](assets/configuration/test-element-hierarchy-compartment.svg){ width="600" }

    === "Test session"

        ![A test session in the test element hierarchy](assets/configuration/test-element-hierarchy-session.svg){ width="600" }

!!! note

    TestBalloon aims to be as composable as possible with a simple but powerful API foundation. It is expected that users can more easily achieve their goals with a small amount of their own customization, rather than by using huge APIs and extension libraries.

## `TestSuite` extensions

To create reusable test variants, you can use extension functions on `TestSuite`.

* A test with a timeout parameter, which also appears in the test's name:

    ```kotlin
    --8<-- "Configuration.kt:test-with-timeout"
    ```

* A reusable test series:

    ```kotlin
    --8<-- "Configuration.kt:test-series"
    ```

* A test providing a database resource as a context:

    ```kotlin
    --8<-- "Configuration.kt:test-with-database-context"
    ```

    1. This annotation makes the IDE plugin aware of the non-standard method signature.
    2. Use a standard Kotlin scope function to safely close the resource after use.
    3. All test actions can now directly invoke database functions via `this`.

Using the same technique, you can create custom test suites, or test suite series.

## `TestConfig`

Use the `testConfig` parameter in conjunction with the `TestConfig` builder to configure any part of the test element hierarchy – your tests, test suites, up to global settings.

```kotlin
--8<-- "Configuration.kt:concurrency"
```

1. Use concurrent test execution instead of the sequential default.
2. Parallelize as needed (and the platform supports).
3. A custom configuration for extra reporting.

### Custom combinations

You can create a custom `TestConfig` extension combining the above configuration

```kotlin
--8<-- "Configuration.kt:custom-test-config-function"
```

1. Starting with `this` enables `TestConfig` method chaining: You build on what was present before.

and then reuse it as follows:

```kotlin
--8<-- "Configuration.kt:reuse-custom-test-config-function"
```

### Custom extensions

You can configure a custom `TestConfig` extension providing a test timeout:

```kotlin
--8<-- "Configuration.kt:custom-test-config-timeout-function"
```

1. Starting with `this`, build on what was present before.
2. Enable real time.
3. Wrap around each test `action()`. By default, you must invoke it at some point, or configure an exception to that rule via `TestConfig.addPermits()`.

The example in [StatisticsReport.kt](https://github.com/infix-de/testBalloon/tree/main/examples/general/src/commonTest/kotlin/com/example/testLibrary/StatisticsReport.kt) shows how to create a more complex custom `TestConfig` extension based on the existing `traversal` function.

You'll be basing a custom extension on one or more existing `TestConfig` functions. The wrappers are good candidates:

* `TestConfig.aroundAll`
* `TestConfig.aroundEach`
* `TestConfig.aroundEachTest`

The [`TestConfig` API documentation](/testBalloon/api/html/testBalloon-framework-core/de.infix.testBalloon.framework.core/-test-config/index.html) provides a complete list.

## Global configuration

`TestSession` and `TestCompartment` are special types of `TestSuite` that form the top of the test element hierarchy. Like any other `TestElement`, they can be configured via `TestConfig`.

### Test compartments

Tests may have different concurrency, isolation and environmental requirements. TestBalloon provides those via `TestCompartment`s. These group top-level test suites, with each compartment running in isolation.

!!! info

    If you use compartments _C1_, _C2_, _C3_, TestBalloon will execute all tests in _C1_, then all tests in _C2_, then all tests in _C3_. The order is not determined, but the isolation between all tests in one compartment against tests in the other compartments is guaranteed.

TestBalloon has a number of predefined compartments:

| Predefined compartment | Configuration of top-level test suites inside the compartment              |
|--|----------------------------------------------------------------------------|
| `TestCompartment.Concurrent` | concurrent/parallel invocation                                             |
| `TestCompartment.Default` | according to `TestSession`'s default configuration                         |
| `TestCompartment.RealTime` | sequential invocation, on a real-time dispatcher, without `TestScope`      |
| `TestCompartment.Sequential` | sequential invocation (useful if `TestSession` is configured differently) |
| `TestCompartment.UI` | sequential invocation, with access to a multiplatform `Main` dispatcher    |

You can use these, or create your own compartments.

#### Choosing the compartment for a test suite

By default, every top-level test suite will be in the `TestSession`'s default compartment. Use the `testSuite` function's `compartment` parameter to put the test suite in a different compartment.

```kotlin
--8<-- "Configuration.kt:test-suite-with-ui-compartment"
```

1. For technical reasons, a compartment assignment must be done lazily.

### Test session

The `TestSession` is a compilation module's root test suite, holding the module-wide default configuration.

By default, TestBalloon uses a `TestSession` with a safe `TestSession.DefaultConfiguration` for all kinds of tests: It will

* execute test elements sequentially
* on `Dispatchers.Default`, and
* use `kotlinx.coroutines.test.TestScope` inside tests.

#### Customization

You can specify your own test session by declaring a class deriving from `TestSession` **inside the test compilation module it should affect**.

!!! tip

    If you want to reuse a custom test session class from a library, put a class deriving from the library's custom test session class into each of your test modules.

To customize a `TestSession`, change its parameters from their defaults.

The `testConfig` parameter defines the global configuration for the entire compilation module. This example extends the framework’s default configuration:

```kotlin
--8<-- "Configuration.kt:custom-test-session"
```

Alternatively, or additionally, you can change the test session's `defaultCompartment`.

If all tests only mutate local state(1), you can speed up test execution greatly by choosing `TestCompartment.Concurrent`:
{ .annotate }

1. Ascertain that tests do not share mutable state among each other and do not access global mutable state.

```kotlin
--8<-- "Configuration.kt:concurrent-test-session"
```

1. For technical reasons, a compartment assignment must be done lazily.
