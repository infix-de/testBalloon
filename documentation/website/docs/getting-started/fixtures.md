A test fixture is a **state holder**. It initializes lazily on first use, and has a lifetime of the test suite it registers in.

!!!note

    The fixture's value is accessed by _invoking_ the fixture. This lets a fixture contain suspending setup code.

```kotlin
--8<-- "Fixtures.kt:starred-users"
```

1. Registers a test fixture with a lifetime of the enclosing test suite.
2. The fixture's setup code can suspend.
3. The fixture's (optional) tear-down code can suspend.
4. The fixture initializes lazily on first use.
5. The second test reuses the same fixture, sharing its setup cost and state.
6. The fixture will close automatically when its suite finishes.

!!! info

    If a fixture's value is an `AutoClosable`, the fixture will use its `close()` method, without requiring `closeWith`.

## Using fixtures

With fixtures, you can create **resource-intensive state** once, reuse it in multiple tests, and rely on resources to be freed when they are no longer needed.

Fixtures also support **tests which build upon each other**, sharing mutable state across tests.

!!! tip

    With its default sequential execution, TestBalloon always runs tests in the order they appear in the source. This makes it easy to write tests which build upon each other.
