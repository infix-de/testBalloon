## Overview

A fixture is a **state holder** for a lazily initialized value, which is to be used in multiple tests.

Fixtures come in two flavors:

* A **test-level fixture** provides a fresh value to each single test. The value has a lifetime of that test.

* A **suite-level fixture** provides a value which is shared across tests within a test suite. The value is created once on first access. It has a lifetime of the test suite it was registered in.

A fixture is created by the `testFixture` function, whose trailing lambda produces the fixture's value. It can be an object of an existing class:

```kotlin
testFixture {
    Account(balance = 42.0)
}
```

Alternatively, the value can be a custom object:

```kotlin
testFixture {
    object {
        val initialBalance = 42.0
        val account = Account(balance = initialBalance)
    }
}
```

!!!note

    The fixture initialization can suspend. In addition, you can use a suspending `closeWith` function for tear-down code (see an example in the section on [suite-level fixtures](#suite-level-fixtures) below). 

## Test-level fixtures

A fixture becomes a test-level fixture by invoking one of the following functions, assuming this class:

```kotlin
--8<-- "Fixtures.kt:test-level-class"
```

1. `asParameterForEach` provides a fresh fixture value as a **parameter** for each test in its scope:

    ```kotlin
    --8<-- "Fixtures.kt:test-level-asParameterForEach"
    ```

2. `asContextForEach` provides a fresh fixture value as a **context** (receiver) for each test in its scope:

    ```kotlin
    --8<-- "Fixtures.kt:test-level-asContextForEach"
    ```

    !!!tip

        Choose a **context** along with an `object` expression to provide multiple properties to tests.

!!!info

    Choose a **test-level fixture** if you want each test to start with the same defined state. This helps to isolate tests from each other. It also makes tests ideal candidates for parallel execution.

## Suite-level fixtures

A fixture becomes a suite-level fixture by invoking one of the following functions, assuming this class:

```kotlin
--8<-- "Fixtures.kt:suite-level-class"
```

1. `asParameterForAll` provides the same fixture value as a **parameter** for all tests in its scope:

    ```kotlin
    --8<-- "Fixtures.kt:suite-level-asParameterForAll"
    ```

    1. Registers a test fixture with a lifetime of the enclosing test suite.
    2. The fixture's setup code can suspend.
    3. The fixture's (optional) tear-down code can suspend.
    4. The fixture initializes lazily on first use.
    5. The second test reuses the same fixture, sharing its setup cost and state.
    6. The fixture will close automatically when its suite finishes.

2. `asContextForAll` provides the same fixture value as a **context** (receiver) for all tests in its scope:

    ```kotlin
    --8<-- "Fixtures.kt:suite-level-asContextForAll"
    ```

    !!!tip

        Choose a **context** along with an `object` expression to provide multiple properties to tests.

3. Simply invoking the fixture also provides its value:

    ```kotlin
    --8<-- "Fixtures.kt:suite-level-invoke"
    ```

    !!!tip

        Invoking the suite-level fixture is practical to provide its value to a test-level fixture. See [this example on mixing test-level setup and shared state](../how-to/migrating-from-junit.md#mixing-test-level-setup-and-shared-state).

!!!info

    Choose a **suite-level fixture** if you want multiple tests to share state. That way, you can **reuse resource-intensive state**. Or you can have **tests that build upon each other**. This is made easy with TestBalloon's default sequential execution, which always runs tests in the order they appear in the source.

## Common characteristics

* A fixture can be a suite-level or a test-level fixture, but not both.
* A fixture's value is always initialized lazily, only when needed.
* A fixture's initialization and tear-down code can suspend. Inside, you can use code which needs a coroutine scope, or directly create additional coroutines via the `testSuiteCoroutineScope` property.
* If a fixture's value is an `AutoClosable`, the fixture will use its `close()` method, without requiring `closeWith`.

!!! note

    Ensure that any coroutines created in a fixture finish or get canceled at the end of the fixture's lifetime. The fixture's test or suite will wait for them.
