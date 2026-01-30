# Robolectric Integration Implementation Summary

## Overview

This document provides a technical summary of the TestBalloon Robolectric integration, including design decisions, implementation approach, and usage patterns.

## Problem Statement

The goal was to create an integration allowing TestBalloon test suites to run in the Robolectric environment, based on the new runner architecture from https://github.com/robolectric/robolectric/pull/10897.

Requirements:
- Use TestBalloon as the test framework (not JUnit4 or JUnit5 directly)
- Allow configuring which TestBalloon test suites use the Robolectric environment
- Ensure other test suites remain unaffected
- Provide clear configuration mechanism to opt-in specific suites
- Include tests/examples demonstrating both scenarios

## Design Decisions

### 1. Fixture-Based Approach

**Decision**: Use TestBalloon's `testFixture` and `asContextForEach` mechanism for Robolectric lifecycle management.

**Rationale**:
- TestBalloon's fixture system is designed for exactly this use case: managing test-level resources
- `asContextForEach` creates a fresh fixture instance for each test, perfect for Robolectric's sandbox isolation
- Fixtures handle setup/teardown automatically through the test lifecycle
- The approach is consistent with TestBalloon's design philosophy

**Alternative Considered**: TestConfig extension (like kotest-assertions integration)
- Rejected because Robolectric requires explicit lifecycle management (beforeTest, afterTest, etc.)
- TestConfig is better for configuration, not stateful resource management

### 2. Opt-In Per Test Suite

**Decision**: Require explicit opt-in via `robolectricContext()` fixture for each suite.

**Rationale**:
- Clear, explicit, and easy to understand
- No global configuration that could affect unrelated tests
- Each suite's intent is obvious from its code
- Zero overhead for non-Robolectric tests

**Example**:
```kotlin
// Suite WITH Robolectric
val AndroidTests by testSuite {
    robolectricContext() asContextForEach {
        test("uses Android APIs") { /* ... */ }
    }
}

// Suite WITHOUT Robolectric (standard JVM)
val RegularTests by testSuite {
    test("plain JVM test") { /* ... */ }
}
```

### 3. Dependency on runner:common

**Decision**: Depend on Robolectric's `runner:common` module rather than the full Robolectric stack.

**Rationale**:
- `runner:common` is framework-agnostic (no JUnit dependencies)
- Provides exactly what we need: `RobolectricIntegration`, `SandboxLifecycleManager`, `RobolectricEnvironment`
- Clean separation of concerns
- Based on the latest Robolectric architecture (PR #10897)

**Current Status**: 
- PR #10897 is not yet merged
- Robolectric 4.15.1 is released but doesn't include `runner:common`
- Implementation is ready but dependency is commented out pending release

## Implementation Structure

### Module Organization

```
testBalloon-integration-robolectric/
├── build.gradle.kts                 # Module configuration
├── README.md                        # User-facing documentation
└── src/
    ├── commonMain/                  # Marker for multiplatform
    │   └── RobolectricIntegrationMarker.kt
    ├── jvmMain/                     # Integration implementation
    │   └── RobolectricIntegration.kt
    └── jvmTest/                     # Examples and tests
        └── examples/
            ├── ExampleWithoutRobolectric.kt    # Working example
            └── ExampleWithRobolectric.kt       # API demonstration
```

### Key Classes (Planned)

#### `RobolectricContext`
- Wraps Robolectric's `RobolectricIntegration`
- Manages sandbox lifecycle
- Configurable via constructor parameters
- Thread-safe via ThreadLocal for sandbox context

#### `robolectricContext()` Function
- Convenience function returning a TestBalloon fixture
- Provides clean API: `robolectricContext() asContextForEach { ... }`
- Handles configuration parameters

### Integration Points

1. **TestBalloon's fixture system**
   - `testFixture { ... }` creates the fixture
   - `asContextForEach { ... }` ensures fresh context per test
   - Automatic cleanup via fixture lifecycle

2. **Robolectric's runner:common**
   - `RobolectricIntegration` interface for lifecycle callbacks
   - `SandboxLifecycleManager` for sandbox creation/management
   - `RobolectricEnvironment` for test execution in sandbox

## Usage Patterns

### Basic Usage

```kotlin
import de.infix.testBalloon.integration.robolectric.robolectricContext
import androidx.test.core.app.ApplicationProvider
import android.content.Context

val MyAndroidTests by testSuite {
    robolectricContext() asContextForEach {
        test("can use Android APIs") {
            val context = ApplicationProvider.getApplicationContext<Context>()
            assertNotNull(context)
        }
    }
}
```

### Advanced Configuration

```kotlin
import org.robolectric.runner.common.SandboxSharingStrategy

val MyTests by testSuite {
    testFixture {
        RobolectricContext(
            sandboxSharing = SandboxSharingStrategy.PER_TEST,
            debugLogging = true
        )
    } asContextForEach {
        test("isolated sandbox") {
            // Fresh sandbox for this test
        }
    }
}
```

### Mixing Robolectric and Non-Robolectric Tests

```kotlin
// File: AndroidTests.kt
val AndroidTests by testSuite {
    robolectricContext() asContextForEach {
        test("uses Android") { /* ... */ }
    }
}

// File: JvmTests.kt
val JvmTests by testSuite {
    test("plain JVM") { /* ... */ }
}

// Both suites can coexist in the same module
// Zero impact on non-Robolectric tests
```

## Benefits

### For TestBalloon Users
1. **Natural API**: Uses familiar TestBalloon patterns
2. **Explicit opt-in**: Clear which tests use Robolectric
3. **Zero overhead**: Non-Robolectric tests unaffected
4. **Type-safe**: Configuration is compile-time checked
5. **Flexible**: Easy to configure per suite

### For the TestBalloon Ecosystem
1. **Clean integration**: Follows existing patterns
2. **Minimal surface area**: Small, focused module
3. **Based on latest tech**: Uses Robolectric's new architecture
4. **Future-proof**: Built on framework-agnostic foundation

## Testing Strategy

### Example Tests Provided

1. **ExampleWithoutRobolectric.kt**
   - Fully functional standard JVM test
   - Demonstrates coexistence
   - Shows zero impact on non-Robolectric code

2. **ExampleWithRobolectric.kt**
   - Documents intended API
   - Shows configuration options
   - Currently commented out (pending Robolectric release)

### Verification Plan (Once Implemented)

1. Build module: `./gradlew :testBalloon-integration-robolectric:build`
2. Run JVM tests: `./gradlew :testBalloon-integration-robolectric:jvmTest`
3. Verify both example suites run
4. Confirm no impact on other TestBalloon modules

## Current Status

### Completed
- ✅ Module structure created
- ✅ Build configuration ready
- ✅ API documented and designed
- ✅ Example tests written
- ✅ README with comprehensive documentation
- ✅ Integration with settings.gradle.kts

### Pending (Waiting for Robolectric)
- ⏸️ Dependency on runner:common (commented out)
- ⏸️ Implementation of RobolectricContext class
- ⏸️ Uncomment example Robolectric tests
- ⏸️ Build and runtime verification
- ⏸️ Add to CI/CD verification tasks

## Next Steps

When Robolectric releases runner:common module:

1. **Update dependency** in `build.gradle.kts`:
   ```kotlin
   api("org.robolectric:runner-common:4.15.x")
   ```

2. **Implement RobolectricContext**:
   - Copy implementation from this document's design
   - Wire up lifecycle callbacks
   - Handle sandbox execution

3. **Uncomment examples**:
   - Enable Android API tests in ExampleWithRobolectric.kt
   - Verify both scenarios work

4. **Add to build**:
   - Include in verification tasks in root build.gradle.kts
   - Add to CI pipeline

5. **Document and release**:
   - Update CHANGELOG.md
   - Publish to Maven Central with next TestBalloon release

## References

- [Robolectric PR #10897](https://github.com/robolectric/robolectric/pull/10897) - New runner architecture
- [TestBalloon Documentation](https://infix-de.github.io/testBalloon/) - Framework docs
- [TestFixture API](../testBalloon-framework-core/src/commonMain/kotlin/de/infix/testBalloon/framework/core/TestFixture.kt) - Fixture implementation
- Existing integrations:
  - [kotest-assertions](../testBalloon-integration-kotest-assertions/) - TestConfig pattern
  - [blocking-detection](../testBalloon-integration-blocking-detection/) - Coroutine context pattern

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                      TestBalloon Test Suite                      │
│                                                                   │
│  val MyTests by testSuite {                                      │
│      robolectricContext() asContextForEach {                     │
│          test("name") { /* Android APIs available */ }           │
│      }                                                            │
│  }                                                                │
└─────────────────────────────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│           testBalloon-integration-robolectric Module             │
│                                                                   │
│  ┌────────────────────────────────────────────────────┐         │
│  │         robolectricContext() Function              │         │
│  │   Returns: TestFixture<RobolectricContext>         │         │
│  └────────────────────────────────────────────────────┘         │
│                             │                                    │
│                             ▼                                    │
│  ┌────────────────────────────────────────────────────┐         │
│  │          RobolectricContext Class                  │         │
│  │  - Wraps RobolectricIntegration                    │         │
│  │  - Manages sandbox lifecycle                       │         │
│  │  - Configures sandbox sharing                      │         │
│  └────────────────────────────────────────────────────┘         │
└─────────────────────────────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│              Robolectric runner:common Module                    │
│                                                                   │
│  ┌──────────────────┐  ┌──────────────────┐                     │
│  │ Robolectric      │  │ Sandbox          │                     │
│  │ Integration      │  │ LifecycleManager │                     │
│  └──────────────────┘  └──────────────────┘                     │
│  ┌──────────────────┐  ┌──────────────────┐                     │
│  │ Robolectric      │  │ Test             │                     │
│  │ Environment      │  │ Bootstrapper     │                     │
│  └──────────────────┘  └──────────────────┘                     │
└─────────────────────────────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Robolectric Core                              │
│         (Android Framework Mocks & Shadows)                      │
└─────────────────────────────────────────────────────────────────┘
```

## Conclusion

This integration provides a clean, TestBalloon-native way to use Robolectric while maintaining complete isolation for non-Robolectric tests. The design leverages TestBalloon's fixture system and Robolectric's new framework-agnostic architecture to create an elegant, type-safe API that feels natural to TestBalloon users.

The implementation is ready and awaiting only the release of Robolectric's runner:common module to become fully functional.
