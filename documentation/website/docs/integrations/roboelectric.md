The Roboelectric integration brings [Roboelectric's fast, reliable and configurable Android testing](https://robolectric.org/) to TestBalloon:

* All TestBalloon capabilities are available in Roboelectric test suites, including parameterized tests, nested test suites and test fixtures.
* The Roboelectric environment can be fully configured in plain Kotlin, using a DSL, without resorting to annotations.

## Getting started

1. Add the integration's dependency for Android host-side tests:

    === "Kotlin Multiplatform with Android host-side tests"

        ```kotlin
        named("androidHostTest") { // (1)!
            dependencies {
                implementation("de.infix.testBalloon:testBalloon-integration-roboelectric:$testBalloonVersion")
                implementation("junit:junit:$junit4Version")
            }
        }
        ```

        1. Using the `com.android.kotlin.multiplatform.library` plugin.

    === "Android-only host-side tests"

        ```kotlin
        dependencies {
            testImplementation("de.infix.testBalloon:testBalloon-integration-roboelectric:$testBalloonVersion")
            testImplementation("junit:junit:$junit4Version")
        }
        ```

2. Register a Roboelectric test suite with [`roboelectricTestSuite`](../api/testBalloon-integration-roboelectric/de.infix.testBalloon.integration.roboelectric/roboelectric-test-suite.html):

    ```kotlin
    --8<-- "Roboelectric.kt:roboelectric-test-suite"
    ```

    1. Registers a Roboelectric test suite. This must occur inside an existing test suite.
    2. Specifies the class which will contain the test suite's content.
    3. Configure Roboelectric as desired. As usual, this can also be done higher up in TestBalloon's [test element hierarchy](../getting-started/configuration.md).
    4. There is no trailing lambda because the test suite's content resides in a separate class.

3. Add the Roboelectric test suite contents to a separate class derived from `RoboelectricTestSuiteContent`:

    ```kotlin
    --8<-- "Roboelectric.kt:roboelectric-test-suite-content"
    ```

    1. This class will be dynamically loaded by Roboelectric's sandbox class loader.
    2. The `RoboelectricTestSuiteContent` base class handles all TestBalloon integration.

    !!! note

        Roboelectric test suites do not nest. You cannot invoke `roboelectricTestSuite` inside `RoboelectricTestSuiteContent`.

## Configuration

[`TestConfig.roboelectric { ... }`](../api/testBalloon-integration-roboelectric/de.infix.testBalloon.integration.roboelectric/roboelectric.html) configures all [Roboelectric settings via a DSL](../api/testBalloon-integration-roboelectric/de.infix.testBalloon.integration.roboelectric/-roboelectric-settings/index.html).

As with any other `TestConfig` configuration, Roboelectric settings can be configured at any level of the [test element hierarchy](../getting-started/configuration.md), including globally. Settings are inherited, but can be overridden at lower levels.

!!! note

    Settings become effective wherever a `roboelectricTestSuite` appears. Settings appearing inside a Roboelectric test suite have no effect.

[`roboelectricTestSuite`](../api/testBalloon-integration-roboelectric/de.infix.testBalloon.integration.roboelectric/roboelectric-test-suite.html) has an `arguments` parameter which you can use to pass values to constructor parameters of the corresponding test suite content class.

!!! info

    `arguments` is the boundary where types and values travel between the "normal" JVM world and the Roboelectric environment. By default, Roboelectric will re-load all classes it encounters with its own sandbox class loader, making them incompatible with the "same" classes in the JVM world.

    To make a class `MyType` and all classes in `com.example.mypackage` portable between those worlds, add the following `testConfig` parameter to the `roboelectricTestSuite` invocation (or anywhere above it in the test element hierarchy):
    
    ```kotlin
    testConfig = TestConfig.roboelectric {
        portableClasses += MyType::class
        portablePackages += "com.example.mypackage."
    }
    ```

## Roboelectric Resources

* [Configuring Robolectric](https://robolectric.org/configuring/)
* [Device Configuration](https://robolectric.org/device-configuration/)
* [Using qualified resources](https://robolectric.org/using-qualifiers/)
* [Best Practices & Limitations](https://robolectric.org/best-practices/)
