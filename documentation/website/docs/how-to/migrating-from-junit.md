## Choose your pace

TestBalloon can reside with JUnit 4/5/6 tests in the same module(1), running tests side-by-side.
{ .annotate }

1. For **Android host-side tests**: If you have JUnit Platform **_and_** JUnit 4 enabled (e.g. by using JUnit Vintage), please disable TestBalloon on JUnit Platform via `useJUnitPlatform { excludeEngines("de.infix.testBalloon") }`. Otherwise, the framework would respond to both integrations and initialize twice, which produces an error.

You can **migrate at your pace**, and you don't need to migrate code that does not benefit from TestBalloon's capabilities.

## Keep your assertions

You can **keep your assertion library** (most assertion libraries work out of the box, for Kotest Assertions there is a TestBalloon [integration for it](../getting-started/first-steps.md/#kotest-assertions)). Code inside tests can remain unchanged.

## What needs to change

### Test Classes and Methods

Top-level test classes become top-level suite properties. Test methods become `test` function invocations:

=== "JUnit"

    ```kotlin
    --8<-- "MigratingFromJUnit.kt:junit-basics"
    ```

=== "TestBalloon"

    ```kotlin
    --8<-- "MigratingFromJUnit.kt:testballoon-basics"
    ```

### Class properties, setup and teardown

Keep your code inside tests, and

1. wrap JUnit class properties into a [fixture](../getting-started/fixtures.md), omitting `var` and `lateinit`,
2. omit `runTest`,
3. co-locate setup code with initialization,
4. use the optional `closeWith` lambda for tear-down code,
5. make it a [test-level fixture](../getting-started/fixtures.md#test-level-fixtures), providing a fresh value to each test:

=== "JUnit"

    ```kotlin
    --8<-- "MigratingFromJUnit.kt:junit-test-level-fixture"
    ```

    1. `@BeforeEach` in JUnit 5+.
    2. `@AfterEach` in JUnit 5+.

=== "TestBalloon"

    ```kotlin
    --8<-- "MigratingFromJUnit.kt:testballoon-test-level-fixture"
    ```

    1. As the `testFixture` lambda is suspending, you can co-locate any setup code here.
    2. A suspending tear-down function.
    3. Making it a test-level fixture provides a fresh, isolated value as a parameter for each test.

!!! tip

    For a fixture with multiple properties, use an `object` expression and provide it via [`asContextForEach` to each test](../getting-started/fixtures.md#test-level-fixtures).

### Sharing state across tests

Keep your code inside tests, and

1. wrap JUnit class properties into a [fixture](../getting-started/fixtures.md), omitting `var`, `lateinit`, and static declarations (companion object),
2. co-locate setup code with initialization,
3. use the optional `closeWith` lambda for tear-down code,
4. make it a [suite-level fixture](../getting-started/fixtures.md#suite-level-fixtures), providing a shared value to all tests:

=== "JUnit"

    ```kotlin
    --8<-- "MigratingFromJUnit.kt:junit-suite-level-fixture"
    ```

    1. `@BeforeAll` in JUnit 5+.
    2. `@AfterAll` in JUnit 5+.

=== "TestBalloon"

    ```kotlin
    --8<-- "MigratingFromJUnit.kt:testballoon-suite-level-fixture"
    ```

    1. Making it a suite-level fixture provides a shared value as a parameter for all tests.

!!! tip

    For a fixture with multiple properties, use an `object` expression and provide it as via [`asContextForAll` to all tests](../getting-started/fixtures.md#suite-level-fixtures).

### Mixing test-level setup and shared state

Use the test-level fixture as [shown above](#class-properties-setup-and-teardown), and use additional shared fixtures for class-level state:

=== "JUnit"

    ```kotlin
    --8<-- "MigratingFromJUnit.kt:junit-mixed-fixture"
    ```

    1. `@BeforeAll` in JUnit 5+.
    2. `@BeforeEach` in JUnit 5+.
    3. `@AfterEach` in JUnit 5+.

=== "TestBalloon"

    ```kotlin
    --8<-- "MigratingFromJUnit.kt:testballoon-mixed-fixture"
    ```

    1. Invoking the `sharedService` fixture makes it a [suite-level fixture](../getting-started/fixtures.md#suite-level-fixtures).

## JUnit 4

### Rules

1. Use a fixture as shown before, but put properties into an object deriving from a `JUnit4RulesContext`.
2. Instead of `@Rule` annotations, register rules via a `rule()` function.

=== "JUnit"

    ```kotlin
    --8<-- "JetpackComposeTests.kt:junit-jetpackCompose"
    ```

=== "TestBalloon"

    ```kotlin
    --8<-- "JetpackComposeTests.kt:testballoon-jetpackCompose"
    ```
    
    1. Deriving a fixture value from `JUnit4RulesContext` enables support for JUnit 4 rules.
    2. Instead of annotations, use the `rule()` function to register a `TestRule`.

!!! tip

    Use only pre-existing rules with TestBalloon, avoid creating new ones. Rules are blocking by nature and do not mesh well with Kotlin's coroutines. Use TestBalloon's [`TestConfig.aroundEachTest()`](../api/testBalloon-framework-core/de.infix.testBalloon.framework.core/around-each-test.html) to wrap code around tests with full coroutine support.

### Parameterized tests [](){ #junit4-parameterized-tests }

1. Drop `@RunWith(Parameterized::class)`, class properties for parameters, and the companion object.
2. Use plain Kotlin:

=== "JUnit"

    ```kotlin
    --8<-- "MigratingFromJUnit.kt:junit-parameterized"
    ```

=== "TestBalloon"

    ```kotlin
    --8<-- "MigratingFromJUnit.kt:testballoon-parameterized"
    ```

## JUnit 5, JUnit 6

### Parameterization

JUnit 5+ offers parameterization via `@ParameterizedClass` and `@MethodSource` annotations. These are very similar to [JUnit 4 parameterized tests](#junit4-parameterized-tests). The same migration techniques apply.

Migrating JUnit 5+ templated tests to TestBalloon follows the same pattern.

### Ordering

To run tests in a chosen order, JUnit 5+ requires interventions (like an `@Order` annotation). In TestBalloon, tests run in the order they appear in the source by default.

### Extensions

JUnit 5+ extensions are reusable classes whose methods can run code before, after, or around tests.

TestBalloon's [TestConfig builder](../api/testBalloon-framework-core/de.infix.testBalloon.framework.core/-test-config/index.html) provides 4 functional mechanisms which achieve the same:

* two universal functions: `aroundEach()` and `traversal()`,
* two convenience variants: `aroundAll()` and `aroundEachTest()`.

=== "JUnit"

    ```kotlin
    --8<-- "MigratingFromJUnit6.kt:junit6-extension"
    ```

=== "TestBalloon"

    ```kotlin
    --8<-- "MigratingFromJUnit6.kt:testballoon-from-junit6-extension"
    ```

### Other

**Nested** and **dynamic tests** are covered as TestBalloon's `testSuite` functions nest and everything is dynamic by nature.

Most other JUnit 5+ features like **disabling tests**, **conditional execution**, **tagging**, **repeated tests** have a natural replacement using plain Kotlin, TestBalloon's [TestConfig builder](../api/testBalloon-framework-core/de.infix.testBalloon.framework.core/-test-config/index.html), and environment variables [(see this example)](effective-testing.md#conditional-tag-based-testing).
