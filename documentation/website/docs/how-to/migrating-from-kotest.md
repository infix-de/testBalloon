## Keep your structure and assertions

Like Kotest, TestBalloon is DSL-based and offers a choice of assertion libraries:

* You can possibly **keep large parts of your test structure** by using some helper functions which translate from one of Kotest's test styles to TestBalloon's `testSuite` and `test` DSL functions.

    !!! tip

        [TestBalloon Addons](https://github.com/a-sit-plus/testballoon-addons) by A-SIT Plus is a ready-to-use library which provides a translation for `FreeSpec`, plus replacements for Kotest's data-driven and property test functions.

* You can **keep your assertion library** (if it is Kotest Assertions, TestBalloon has [an integration for it](../getting-started/first-steps.md/#kotest-assertions)).

## What needs to change

### Specs

Kotest's specs become TestBalloon's top-level test suites:

=== "Kotest"

    ```kotlin hl_lines="1 6"
    class MyTests : FunSpec({
        test("String length should return the length of the string") {
            "sammy".length shouldBe 5
            "".length shouldBe 0
        }
    })
    ```

=== "TestBalloon"

    ```kotlin hl_lines="1 6"
    val MyTests by testSuite {
        test("String length should return the length of the string") {
            "sammy".length shouldBe 5
            "".length shouldBe 0
        }
    }
    ```

### Isolation Modes

You can keep all code in Kotest's default _Single Instance_ mode.

For Kotest's `InstancePerRoot` and `InstancePerLeaf` isolation modes, keep tests as they are, but wrap them into a [test-level fixture](../getting-started/fixtures.md#test-level-fixtures):

=== "Kotest"

    ```kotlin
    class IsolatedTests : FunSpec({
        isolationMode = IsolationMode.InstancePerLeaf // (1)!

        val id = UUID.randomUUID() // (2)!

        init {
            test("one") {
                println(id) // (3)!
            }
            test("two") {
                println(id) // (4)!
            }
        }
    })
    ```

    1. Kotest 6 deprecates `InstancePerLeaf` in favor of `InstancePerRoot`, but the latter isolates only one level of tests.
    2. The test context is initialized by re-creating the spec from its class via reflection (JVM-only).
    3. Each test prints a different ID.
    4. Each test prints a different ID.

=== "TestBalloon"

    ```kotlin
    --8<-- "MigratingFromKotest.kt:isolated-tests"
    ```

    1. The fixture's value can be any Kotlin type. You can also use an object expression with multiple properties.
    2. This test-level fixture provides a fresh, isolated parameter for each test.
    3. Each test prints a different ID.
    4. Each test prints a different ID.

!!! note

    When migrating existing code, please remember the concepts of [Green code and blue code](../getting-started/tests-and-suites.md#green-code-and-blue-code) and [TestBalloon's golden rule](../getting-started/tests-and-suites.md#testballoons-golden-rule).

!!! tip

    Sharing state via TestBalloon's [suite-level fixtures](../getting-started/fixtures.md#suite-level-fixtures) may help you squeeze complexity out of existing code, which did not have access to this mechanism.

### Lifecycle Hooks

Replace Kotest's lifecycle hooks with TestBalloon's wrappers, like in this re-use example:

=== "Kotest"

    ```kotlin
    val printStart: BeforeTest = {
        println("Starting a test $it")
    }
    
    class LifecycleHookTests : FunSpec({
        beforeTest(printStart)
        test("this test should be alive") {
            println("Johnny5 is alive!")
        }
    })
    ```

=== "TestBalloon"

    ```kotlin
    --8<-- "MigratingFromKotest.kt:lifecycle-hooks"
    ```

    1. You can put extra code after the test action, or surround it with a timeout function, or a try-catch, or…

Kotest has 44 mechanisms to track and influence tests:

* 14 "lifecycle hooks",
* 14 "simple extensions", and
* 16 "advanced extensions".

TestBalloon's [TestConfig decorator chain](../api/testBalloon-framework-core/de.infix.testBalloon.framework.core/-test-config/index.html) provides 4 functional mechanisms which achieve the same:

* two universal functions: `aroundEach()` and `traversal()`,
* two convenience variants: `aroundAll()` and `aroundEachTest()`.

!!! note

    TestBalloon's `TestConfig` wrappers can be combined and attached to any point in the test element hierarchy. `TestConfig` is a unified and composable mechanism, available for all kinds of test elements. Like the Modifiers of Jetpack Compose, this design make the API concise and flexible.

## Extensions

### Data-driven testing

TestBalloon uses plain Kotlin for data driven tests. There is no additional API to learn.

If you want to keep using **Kotest's data-driven testing** features, or its **property testing**, the [TestBalloon Addons](https://github.com/a-sit-plus/testballoon-addons) library by A-SIT Plus provides both.
