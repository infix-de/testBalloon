## Keep your structure and assertions

Like Kotest, TestBalloon is DSL-based and offers a choice of assertion libraries:

* You can possibly **keep large parts of your test structure** by using some helper functions which translate from one of Kotest's test styles to TestBalloon's `testSuite` and `test` DSL functions.

    !!! tip

        [TestBalloon Addons](https://github.com/a-sit-plus/testballoon-addons) by A-SIT Plus is a ready-to-use library which provides a translation for `FreeSpec`.

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

Code using Kotest's other isolation modes must change to explicit initialization:

=== "Kotest"

    ```kotlin
    class IsolatedTests : FunSpec({
        isolationMode = IsolationMode.InstancePerTest // (4)!

        val id = UUID.randomUUID() // (1)!

        init {
            test("one") {
                println(id) // (2)!
            }
            test("two") {
                println(id) // (3)!
            }
        }
    })
    ```

    1. The test context is initialized by re-creating the spec from its class via reflection (JVM-only).
    2. Each test prints a different ID.
    3. Each test prints a different ID.
    4. Kotest 6 deprecates `InstancePerTest` in favor of `InstancePerRoot`, but the latter isolates only one level of tests.

=== "TestBalloon"

    ```kotlin
    --8<-- "MigratingFromKotest.kt:isolated-tests"
    ```

    1. The test context is initialized explicitly (multiplatform-compatible).
    2. Each test prints a different ID.
    3. Each test prints a different ID.
    4. A helper function declaring a custom test, as explained in the [effective testing](effective-testing.md#supply-fresh-state-to-multiple-tests) guide.

!!! note

    When migrating existing code, please remember the concepts of [Green code and blue code](../getting-started/tests-and-suites.md#green-code-and-blue-code) and [TestBalloon's golden rule](../getting-started/tests-and-suites.md#testballoons-golden-rule).

!!! tip

    Sharing state via TestBalloon's fixtures may help you squeeze complexity out of existing code, which did not have access to this mechanism.

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

TestBalloon's [TestConfig builder](../api/testBalloon-framework-core/de.infix.testBalloon.framework.core/-test-config/index.html) provides 4 mechanisms which achieve the same:

* two functions: `aroundEach()` and `traversal()`,
* two convenience variants: `aroundAll()` and `aroundEachTest()`.

!!! note

    TestBalloon's `TestConfig` wrappers can be combined and attached to any point in the test element hierarchy. `TestConfig` is a unified and composable mechanism, available for all kinds of test elements. Like the Modifiers of Jetpack Compose, this design make the API concise and flexible.

## Extensions

### Data-driven testing

TestBalloon uses plain Kotlin for data driven tests. There is no additional API to learn.

If you want to keep using **Kotest's data-driven testing** features, or its **property testing**, the [TestBalloon Addons](https://github.com/a-sit-plus/testballoon-addons) library by A-SIT Plus provides both.
