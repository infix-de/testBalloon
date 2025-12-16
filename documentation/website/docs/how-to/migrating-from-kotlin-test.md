## Keep your assertions

You can **keep your assertion library** (most assertion libraries work out of the box, for Kotest Assertions there is a TestBalloon [integration for it](../getting-started/first-steps.md/#kotest-assertions)). Code inside tests can remain unchanged.

## What needs to change

### Test Classes and Methods

Top-level test classes become top-level suite properties. Test methods become `test` function invocations:

=== "kotlin.test"

    ```kotlin
    --8<-- "MigratingFromKotlinTest.kt:kotlinTest-basics"
    ```

=== "TestBalloon"

    ```kotlin
    --8<-- "MigratingFromKotlinTest.kt:testballoon-basics"
    ```

### Class properties, setup and teardown

Keep your code inside tests, and

1. wrap `kotlin.test` class properties into a [fixture](../getting-started/fixtures.md), omitting `var` and `lateinit`,
2. omit `runTest`,
3. co-locate setup code with initialization,
4. use the optional `closeWith` lambda for tear-down code,
5. make it a [test-level fixture](../getting-started/fixtures.md#test-level-fixtures), providing a fresh value to each test:

=== "kotlin.test"

    ```kotlin
    --8<-- "MigratingFromKotlinTest.kt:kotlinTest-test-level-fixture"
    ```

=== "TestBalloon"

    ```kotlin
    --8<-- "MigratingFromKotlinTest.kt:testballoon-test-level-fixture"
    ```

    1. As the `testFixture` lambda is suspending, you can co-locate any setup code here.
    2. A suspending tear-down function.
    3. Making it a test-level fixture provides a fresh, isolated value as a parameter for each test.

!!! tip

    For a fixture with multiple properties, use an `object` expression and provide it via [`asContextForEach` to each test](../getting-started/fixtures.md#test-level-fixtures).

### Other

To `@Ignore` a test or suite, pass `testConfig = TestConfig.disable()` as a parameter to the `test` or `testSuite` function.

To migrate `@BeforeClass` and `@AfterClass` (which `kotlin.test` provides on Native), see the corresponding [JUnit section on sharing state across tests](migrating-from-junit.md#sharing-state-across-tests). 
