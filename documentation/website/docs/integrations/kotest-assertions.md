The Kotest Assertions integration provides support for the [Kotest Assertions library](https://kotest.io/docs/assertions/assertions.html) with its large selection of matchers.

!!! info

    Some Kotest Assertions functions, like `assertSoftly` and `withClue`, require a special setup to work safely with multithreaded coroutines. This integration provides such setup.

    Otherwise, Kotest Assertions can be used out of the box without this integration.

## Getting started

1. Add the integration's dependency to the common or other test source set:

    ```kotlin
    implementation("de.infix.testBalloon:testBalloon-integration-kotest-assertions:$testBalloonVersion")
    ```

2. Use the following `testConfig` parameter at the appropriate point in your [test element hierarchy](../getting-started/configuration.md), for example the [`TestSession`](../getting-started/configuration.md#test-session):

    ```kotlin
    testConfig = TestConfig. kotestAssertionsSupport()
    ```

That's it. There is nothing else to configure.
