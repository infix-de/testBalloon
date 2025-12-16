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

    === "Kotlin Multiplatform with Android host-side tests"

        ```kotlin
        named("androidHostTest") { // (1)!
            dependencies {
                implementation("de.infix.testBalloon:testBalloon-framework-core:$testBalloonVersion")
                implementation("junit:junit:$junit4Version")
            }
        }
        ```

        1. Using the `com.android.kotlin.multiplatform.library` plugin.

    === "Kotlin Multiplatform with Android device-side tests"

        ```kotlin
        named("androidDeviceTest") { // (1)!
            dependencies {
                implementation("de.infix.testBalloon:testBalloon-framework-core:$testBalloonVersion")
                implementation("androidx.test:runner:$androidxRunnerVersion")
            }
        }
        ```

        1. Using the `com.android.kotlin.multiplatform.library` plugin.

    === "Android-only host-side tests"

        ```kotlin
        dependencies {
            testImplementation("de.infix.testBalloon:testBalloon-framework-core:$testBalloonVersion")
            testImplementation("junit:junit:$junit4Version")
        }
        ```

    === "Android-only device-side tests"

        ```kotlin
        dependencies {
            androidTestImplementation("de.infix.testBalloon:testBalloon-framework-core:$testBalloonVersion")
            androidTestImplementation("androidx.test:runner:$androidxRunnerVersion")
        }
        ```

    !!! info

        The repository contains configuration examples for [Android](https://github.com/infix-de/testBalloon/tree/main/examples/android/build.gradle.kts) and [Multiplatform library with Android](https://github.com/infix-de/testBalloon/tree/main/examples/multiplatform-library-with-android/build.gradle.kts).

4. Add a dependency for the assertions library of your choice:

    === "kotlin.test assertions"

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
