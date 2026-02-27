---
date: 2026-02-24
links:
  - Robolectric integration: integrations/robolectric.md
---

# Robolectric meets TestBalloon

GenAI said it would be impossible:

> This integration is not feasible with the current TestBalloon architecture without significant changes to the compiler plugin or framework core.

Yet here it is – no significant changes, just an integration built with the public TestBalloon API.

## So what do we have?

[Robolectric](https://robolectric.org/) brings fast, reliable and configurable Android tests to the JVM. It lets us operate close to the real device without waiting for dexing, packaging, installing and emulator start-up.

TestBalloon adds easily parameterized tests, nested test suites, test fixtures, and a Robolectric environment that can be fully configured in plain Kotlin, using a DSL, avoiding the restrictions of annotations.

<!-- more -->

### An example

To test our app's rendering on a set of display formats combined with several Android API levels, we'd write:

```kotlin
val RenderingTests by testSuite {
    for (display in listOf("xlarge-port", "xlarge-land")) {
        for (apiLevel in listOf(36, 34, 28)) {
            robolectricTestSuite( // (1)!
                "Display: $display, API $apiLevel",
                RenderingTestSuiteContent::class, // (2)!
                testConfig = TestConfig.robolectric { // (3)!
                    sdk = apiLevel
                    qualifiers = display
                }
            ) // (4)!
        }
    }
}
```

1. This invocation creates a special kind of test suite for Robolectric.
2. Test suites and tests inside the Robolectric environment reside in their own class.
3. We use the usual TestBalloon configuration mechanism.
4. Because the Robolectric test suite's content resides in its own class, there is no trailing lambda.

`RenderingTestSuiteContent` contains the corresponding test suites and tests, which execute for each combination (6 in total):

```kotlin
class RenderingTestSuiteContent : RobolectricTestSuiteContent({
    test("Send button appears") {
        // Android API calls ...
    }

    testSuite("Content") {
        test("At least three cards display") {
            // Android API calls ...
        }

        // ...
    }
})
```

Using the above code, we can now verify that

* our rendering works as intended on the targeted display formats, and
* everything involved works across the specified Android API levels.

## Configuration

`TestConfig.robolectric` lets us configure Robolectric settings which we would traditionally set via Robolectric's `@Config` annotation:

* Everything is configured in plain Kotlin.
* TestBalloon's usual configuration hierarchy applies, which means that settings can be configured at any level of the [test element hierarchy](../../getting-started/configuration.md), including globally.

The list of settings comprises:

| Property               | Type                      | Description                                                                                               |
|------------------------|---------------------------|-----------------------------------------------------------------------------------------------------------|
| `sdk`                  | `Int`                     | The Android SDK level to emulate                                                                          |
| `fontScale`            | `Float`                   | The default font scale                                                                                    |
| `application`          | `KClass<out Application>` | The Application class to use in the test                                                                  |
| `qualifiers`           | `String`                  | Qualifiers specifying device configuration and resource resolution, such as "fr-normal-port-hdpi"         |
| `shadows`              | `MutableSet<KClass<*>>`   | A set of shadow classes to enable, in addition to those that are already present                          |
| `instrumentedPackages` | `MutableSet<String>`      | A set of instrumented packages to enable, in addition to those that are already present                   |
| `portableClasses`      | `MutableSet<KClass<*>>`   | Classes specified to be portable between Robolectric and the outside JVM                                 |
| `portablePackages`     | `MutableSet<String>`      | Packages whose declarations are specified to be portable between Robolectric and the outside JVM         |
| `conscryptMode`        | `ConscryptMode.Mode`      | The mode of choosing a security provider (can be used to prefer Bouncy Castle over the default Conscrypt) |
| `applicationLifetime`  | `ApplicationLifetime`     | The lifetime of an application (the default is per test)                                                  |

## How it works

Robolectric runs its tests in a sandboxed Android environment. It does so by loading classes via a special sandbox class loader, modifying their byte code on the way. That means Android test code has to be packaged in special classes, while code outside such classes should remain untouched.

Although TestBalloon uses a classless, functional approach throughout, we can reconcile the two paradigms:

1. We declare subclasses of `RobolectricTestSuiteContent`, whose only constructor parameter is a lambda function registering the test suite's content – just like the trailing lambda of `testSuite`.
2. `robolectricTestSuite` then auto-wires the test suite content into TestBalloon's test element hierarchy and feeds the class to Robolectric. Everything outside and inside that class remains plain Kotlin with the full TestBalloon API, so nested test suites and other features work as usual.

## Bridging API worlds

Robolectric introduces a new API world with each sandbox it creates. Sandboxes are isolated from each other, which is a welcome side effect. **But sandboxes also differ from the outside JVM world.** By default, Robolectric loads all classes it encounters via its sandbox classloader.

You might never notice that this happens, but if you're curious, expand the following note to read about possible technical consequences.

??? note

    ### Unintended consequences
    
    If we have a regular Kotlin class `com.example.MyClass`, Robolectric might reload that class inside a sandbox. Suddenly our inside class has the same name, but a second incarnation under a different identity(1). If this happens, we can enjoy strange errors like this:
    { .annotate }
    
    1. The identity of a class consists of its fully qualified name _and_ its classloader.
    
    !!! failure
    
        ```
        class com.example.MyClass cannot be cast to class com.example.MyClass (com.example.MyClass is in unnamed module of loader 'app'; com.example.MyClass is in unnamed module of loader org.robolectric.internal.AndroidSandbox$SdkSandboxClassLoader @3bcd426c)
        ```
    
    ### When does this matter?
    
    In most cases, we don't pass data from the outside world into a Robolectric test suite. Test suites and tests outside Robolectric will have their shared parameterization and fixtures, and so will test suites and tests inside a Robolectric test suite, with very little, if any, overlap.
    
    But if the need arises, we can pass data between those worlds. We can declare constructor parameters in our subclass of `RobolectricTestSuiteContent` and provide their values via the `arguments` parameter of `robolectricTestSuite`.
    
    In these rare cases, we might see the above error. But fortunately, TestBalloon provides an easy way out: We can specify `portableClasses` or `portablePackages` in `TestConfig.robolectric`. Robolectric will not touch portable classes, which can then act as conduits between the outside world and Robolectric sandboxes. Just remember to specify them, so that TestBalloon knows.

## The best of both worlds

With the release of the TestBalloon Robolectric integration, we no longer have to choose between restrictive, annotation-based APIs or slow and flaky emulator-based test execution. Combining the ease and power of TestBalloon with fast and reliable Robolectric execution, we can finally have our cake and eat it, too.
