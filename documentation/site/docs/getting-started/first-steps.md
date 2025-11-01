1. Add the TestBalloon Gradle plugin to your build script:

    ```kotlin
    plugins {
        id("de.infix.testBalloon") version "$testBalloonVersion"
    }
    ```

2. Add a dependency for the TestBalloon framework core library:

    === "Kotlin Multiplatform"

        ```kotlin
        commonTest {
            dependencies {
                implementation("de.infix.testBalloon:testBalloon-framework-core:$testBalloonVersion")
            }
        }
        ```

    === "Kotlin JVM"

        ```kotlin
        dependencies {
            implementation("de.infix.testBalloon:testBalloon-framework-core:$testBalloonVersion")
        }
        ```

    !!! info

        The repository contains a [Multiplatform configuration example](https://github.com/infix-de/testBalloon/tree/main/examples/general/build.gradle.kts).

3. Add extra dependencies for Android (optional):

    === "Kotlin Multiplatform with Android local tests"

        ```kotlin
        androidUnitTest {
            dependencies {
                implementation("de.infix.testBalloon:testBalloon-framework-core-jvm:$testBalloonVersion")
            }
        }
        ```

    === "Kotlin Multiplatform with Android instrumented tests"

        ```kotlin
        androidInstrumentedTest {
            dependencies {
                implementation("androidx.test:runner:$androidxRunnerVersion")
                implementation("de.infix.testBalloon:testBalloon-framework-core:$testBalloonVersion")
            }
        }
        ```

    === "Android-only local tests"

        ```kotlin
        dependencies {
            testImplementation("de.infix.testBalloon:testBalloon-framework-core-jvm:$testBalloonVersion")
        }
        ```

    === "Android-only instrumented tests"

        ```kotlin
        dependencies {
            androidTestImplementation("androidx.test:runner:$androidxRunnerVersion")
            androidTestImplementation("de.infix.testBalloon:testBalloon-framework-core:$testBalloonVersion")
        }
        ```

    !!! info

        The repository contains configuration examples for [Android](https://github.com/infix-de/testBalloon/tree/main/examples/android/build.gradle.kts), [Multiplatform with Android](https://github.com/infix-de/testBalloon/tree/main/examples/multiplatform-with-android/build.gradle.kts) and [Multiplatform library with Android](https://github.com/infix-de/testBalloon/tree/main/examples/multiplatform-library-with-android/build.gradle.kts).

    !!! note

        Google is deprecating the `com.android.application` plugin in favor of `com.android.kotlin.multiplatform.library`. Android-KMP integration is currently a moving target and there are multiple issues, so please test if it meets your needs.

4. Add a dependency for the assertions library of your choice:

    === "kotlin-test assertions"

        ```kotlin
        implementation(kotlin("test"))
        ```

    === "Kotest assertions"

        ```kotlin
        implementation("de.infix.testBalloon:testBalloon-integration-kotest-assertions:$testBalloonVersion")
        ```

5. Write a test:

    ```kotlin
    --8<-- "FirstSteps.kt:my-first-test-suite"
    ```

6. Run tests via the familiar Gradle test tasks.

7. Install the [TestBalloon plugin for IntelliJ IDEA] from the JetBrains Marketplace to run individual tests or test suites via the editor’s gutter icons.

[TestBalloon plugin for IntelliJ IDEA]: https://plugins.jetbrains.com/plugin/27749-testballoon
