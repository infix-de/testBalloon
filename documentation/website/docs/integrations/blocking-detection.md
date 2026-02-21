The Blocking Detection integration helps to detect inadvertent calls of blocking code by coroutines.

Example: Calling an I/O library function blocks a CPU-bound thread.

!!! info

    Blocking detection is available exclusively on the JVM via [BlockHound](https://github.com/reactor/BlockHound). In can be enabled in common code, but has no effect on non-JVM platforms.

## Getting started

1. Add the integration's dependency to the common or JVM test source set:

    ```kotlin
    implementation("de.infix.testBalloon:testBalloon-integration-blocking-detection:$testBalloonVersion")
    ```

2. Use the following `testConfig` parameter at the appropriate point in your [test element hierarchy](../getting-started/configuration.md):

    ```kotlin
    testConfig = TestConfig.blockingDetection()
    ```

## Configuration

By default, the Blocking Detection integration throws an error for suspicious blocking calls. To print detected blocking calls instead, use the following:

```kotlin
testConfig = TestConfig.blockingDetection(BlockingDetection.PRINT)
```

Blocking Detection can also be disabled:

```kotlin
testConfig = TestConfig.blockingDetection(BlockingDetection.DISABLED)
```

## Detection

Blocking calls will be detected in coroutine threads which are expected not to block. Such threads are created by the default dispatcher as this example demonstrates:

```kotlin
private suspend fun blockInNonBlockingContext() {
    withContext(Dispatchers.Default) {
        @Suppress("BlockingMethodInNonBlockingContext")
        Thread.sleep(2)
    }
}
```

The BlockHound integration will by default produce an exception like this whenever it detects a blocking call:

```
reactor.blockhound.BlockingOperationError: Blocking call! java.lang.Thread.sleep0
    at de.infix.testBalloon.integration.blockingDetection.TestBalloonBlockHoundIntegration.applyTo$lambda$0$0(Integration.jvm.kt:78)
    at reactor.blockhound.BlockHound$Builder.lambda$install$8(BlockHound.java:488)
    at reactor.blockhound.BlockHoundRuntime.checkBlocking(BlockHoundRuntime.java:89)
    at java.base/java.lang.Thread.sleep0(Thread.java)
    at java.base/java.lang.Thread.sleep(Thread.java:509)
```

Whenever a blocking call is detected, you can

* replace the call with a non-blocking one (using a coroutine-aware library), or
* schedule the calling coroutine to run on a separate I/O thread (e.g. via `Dispatchers.IO`), or
* add an exception if the blocking is harmless (see below).

## Customization

To customize BlockHound, familiarize yourself with the [BlockHound documentation](https://github.com/reactor/BlockHound/blob/master/docs/README.md).

Exceptions for blocking calls considered harmless can be added via a separate `BlockHoundIntegration` class like this:

```kotlin
import reactor.blockhound.BlockHound
import reactor.blockhound.integration.BlockHoundIntegration

class MyBlockHoundIntegration : BlockHoundIntegration {
    override fun applyTo(builder: BlockHound.Builder): Unit = with(builder) {
        allowBlockingCallsInside("org.slf4j.LoggerFactory", "performInitialization")
    }
}
```

In order to allow `BlockHound` to auto-detect and load the integration, add its fully qualified class name to a service provider configuration file
`resources/META-INF/services/reactor.blockhound.integration.BlockHoundIntegration`.
