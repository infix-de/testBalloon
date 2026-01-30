# TestBalloon Robolectric Integration

This module provides seamless integration between [TestBalloon](https://infix-de.github.io/testBalloon/) and [Robolectric](http://robolectric.org/), enabling Android framework APIs to be tested on the JVM without requiring an actual device or emulator.

## Features

- ✅ **Opt-in per test suite**: Choose which test suites run with Robolectric
- ✅ **Zero impact on other tests**: Non-Robolectric test suites remain completely unaffected
- ✅ **TestBalloon-native API**: Uses TestBalloon's fixture system for natural integration
- ✅ **Flexible configuration**: Customize sandbox sharing, parameter injection, and logging
- ✅ **Based on latest Robolectric**: Uses the new runner architecture from [robolectric/robolectric#10897](https://github.com/robolectric/robolectric/pull/10897)

## Installation

Add the integration to your Gradle dependencies:

```kotlin
dependencies {
    // For JVM tests
    testImplementation("de.infix.testBalloon:testBalloon-integration-robolectric:<version>")
    
    // For Android host-side tests
    testImplementation("de.infix.testBalloon:testBalloon-integration-robolectric:<version>")
}
```

## Quick Start

### Basic Usage

Enable Robolectric for a specific test suite using the `robolectricContext()` fixture:

```kotlin
import de.infix.testBalloon.framework.core.testSuite
import de.infix.testBalloon.integration.robolectric.robolectricContext
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlin.test.assertNotNull

val MyAndroidTests by testSuite {
    robolectricContext() asContextForEach {
        test("can access Android context") {
            val context = ApplicationProvider.getApplicationContext<Context>()
            assertNotNull(context)
        }
        
        test("can create Android components") {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val packageName = context.packageName
            assertNotNull(packageName)
        }
    }
}
```

### Advanced Configuration

Customize the Robolectric environment with various options:

```kotlin
import de.infix.testBalloon.integration.robolectric.RobolectricContext
import org.robolectric.runner.common.SandboxSharingStrategy

val MyAdvancedTests by testSuite {
    testFixture {
        RobolectricContext(
            sandboxSharing = SandboxSharingStrategy.PER_TEST,  // New sandbox per test
            debugLogging = true,                                // Enable debug logs
            metricsEnabled = true                               // Collect performance metrics
        )
    } asContextForEach {
        test("isolated test") {
            // Each test gets a fresh sandbox
        }
    }
}
```

### Mixing Robolectric and Non-Robolectric Tests

Test suites with and without Robolectric can coexist in the same module:

```kotlin
// Suite 1: WITH Robolectric
val AndroidTests by testSuite {
    robolectricContext() asContextForEach {
        test("uses Android APIs") {
            val context = ApplicationProvider.getApplicationContext<Context>()
            assertNotNull(context)
        }
    }
}

// Suite 2: WITHOUT Robolectric (standard JVM test)
val RegularJvmTests by testSuite {
    test("plain JVM test") {
        assertEquals(4, 2 + 2)
    }
}
```

## Configuration Options

### Sandbox Sharing Strategy

Controls how Robolectric sandboxes are shared across tests:

- **`SandboxSharingStrategy.PER_CLASS`** (default): One sandbox per test class
  - Faster test execution
  - Tests share the same Android environment
  - Good for most use cases

- **`SandboxSharingStrategy.PER_TEST`**: New sandbox for each test
  - Maximum isolation
  - Slower test execution
  - Use when tests have conflicting global state

### Parameter Injection

Custom parameter resolvers can be provided for test method parameter injection:

```kotlin
import org.robolectric.runner.common.ParameterResolver

val customResolver = object : ParameterResolver {
    override fun <T : Any> resolve(testClass: Class<*>, parameterType: Class<T>): T? {
        // Custom parameter injection logic
        return null
    }
}

testFixture {
    RobolectricContext(parameterResolver = customResolver)
} asContextForEach { ... }
```

### Debug Logging

Enable verbose logging to troubleshoot issues:

```kotlin
robolectricContext(debugLogging = true)
```

Or via system property:
```
-Drobolectric.runner.debug=true
```

### Metrics Collection

Enable performance metrics collection:

```kotlin
robolectricContext(metricsEnabled = true)
```

Or via system property:
```
-Drobolectric.runner.metrics=true
```

## How It Works

The integration uses TestBalloon's fixture system to manage the Robolectric lifecycle:

1. **Suite Setup**: When a test suite with `RobolectricContext` starts, the Robolectric sandbox is initialized
2. **Test Execution**: Each test runs within the sandbox with Android APIs available
3. **Test Teardown**: After each test, the environment is reset for isolation
4. **Suite Teardown**: When the suite completes, the sandbox is torn down

The lifecycle is automatically managed by TestBalloon's `asContextForEach` mechanism, ensuring proper setup and cleanup.

## Requirements

- **Kotlin**: 2.0 or higher
- **TestBalloon**: Same version as this integration module
- **Robolectric**: 4.15-SNAPSHOT or higher (with runner:common module)
- **JVM Target**: Java 11 or higher
- **Android SDK**: Configured via `ANDROID_HOME` environment variable

## Architecture

This integration is built on top of Robolectric's new runner architecture (PR #10897), which provides:

- **Framework-agnostic API**: The `runner:common` module has no JUnit dependencies
- **Flexible sandbox lifecycle**: Multiple strategies for sharing sandboxes
- **Observable execution**: Built-in logging and metrics
- **Clean separation**: Test framework concerns separated from Robolectric concerns

The `RobolectricContext` class wraps Robolectric's `RobolectricIntegration` interface and adapts it to TestBalloon's fixture model.

## Examples

See the module's test suite for more examples:

- [Example test suite WITH Robolectric](src/jvmTest/kotlin/de/infix/testBalloon/integration/robolectric/ExampleWithRobolectric.kt)
- [Example test suite WITHOUT Robolectric](src/jvmTest/kotlin/de/infix/testBalloon/integration/robolectric/ExampleWithoutRobolectric.kt)

## Troubleshooting

### ClassNotFoundException for Android classes

Ensure your `build.gradle.kts` includes the Android SDK:

```kotlin
dependencies {
    testImplementation("com.google.android:android:36")
}
```

### Tests hang or timeout

Try using `SandboxSharingStrategy.PER_TEST` to isolate tests:

```kotlin
robolectricContext(sandboxSharing = SandboxSharingStrategy.PER_TEST)
```

### ANDROID_HOME not found

Set the `ANDROID_HOME` environment variable to your Android SDK location:

```bash
export ANDROID_HOME=/path/to/android/sdk
```

## Contributing

Contributions are welcome! Please ensure:

1. Tests pass: `./gradlew :testBalloon-integration-robolectric:jvmTest`
2. Code is formatted: `./gradlew :testBalloon-integration-robolectric:lintKotlin`
3. Examples demonstrate both with and without Robolectric scenarios

## License

This module is licensed under the Apache License 2.0, same as the main TestBalloon project.

## References

- [TestBalloon Documentation](https://infix-de.github.io/testBalloon/)
- [Robolectric](http://robolectric.org/)
- [Robolectric PR #10897 - New Runner Architecture](https://github.com/robolectric/robolectric/pull/10897)
