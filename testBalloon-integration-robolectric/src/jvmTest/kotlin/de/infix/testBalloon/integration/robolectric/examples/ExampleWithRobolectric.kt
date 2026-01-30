package de.infix.testBalloon.integration.robolectric.examples

import de.infix.testBalloon.framework.core.testSuite

/**
 * Example of a test suite that WOULD use Robolectric (if it were available).
 *
 * This demonstrates the intended API for enabling Robolectric in a TestBalloon test suite.
 * Once Robolectric 4.15 is released with the runner:common module, this code will work.
 *
 * ## What this demonstrates:
 * - How to opt-in a specific test suite to use Robolectric
 * - The `robolectricContext()` fixture provides Android framework APIs
 * - Tests can use Android classes like Context, Activity, View, etc.
 * - The fixture is applied with `asContextForEach` to get a fresh environment per test
 *
 * ## Current Status:
 * This code is currently commented out because Robolectric 4.15 is not yet released.
 * Uncomment once the dependency is available.
 */
val ExampleWithRobolectric by testSuite {
    /*
    // Uncomment once Robolectric 4.15 is available:
    
    robolectricContext() asContextForEach {
        test("can create Android context") {
            val context = ApplicationProvider.getApplicationContext<Context>()
            assertNotNull(context)
            assertNotNull(context.packageName)
        }

        test("can use Android resources") {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val displayMetrics = context.resources.displayMetrics
            assertNotNull(displayMetrics)
        }

        test("can build activities") {
            val controller = Robolectric.buildActivity(Activity::class.java)
            val activity = controller.create().start().resume().get()
            assertNotNull(activity)
            assertEquals(Lifecycle.State.RESUMED, activity.lifecycle.currentState)
        }
    }
    */
    
    // Placeholder test to make the suite valid
    test("placeholder - demonstrates intended Robolectric usage") {
        // See the commented code above for the intended usage once Robolectric 4.15 is available
        println("This test suite will use Robolectric once it's available")
    }
}

/**
 * Configuration options for Robolectric integration (demonstration).
 *
 * Once implemented, you'll be able to configure:
 */
@Suppress("unused")
private val ExampleWithCustomConfiguration by testSuite {
    /*
    // Uncomment once implemented:
    
    testFixture {
        RobolectricContext(
            sandboxSharing = SandboxSharingStrategy.PER_TEST,  // Fresh sandbox per test
            debugLogging = true                                  // Enable verbose logging
        )
    } asContextForEach {
        test("each test gets isolated sandbox") {
            // Changes to Android state won't affect other tests
        }
    }
    */
    
    test("placeholder for configuration example") {
        println("See commented code for configuration options")
    }
}
