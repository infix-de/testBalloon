package de.infix.testBalloon.integration.robolectric

/**
 * Placeholder for Robolectric integration with TestBalloon.
 *
 * **Note**: This integration requires Robolectric 4.15 or later, which includes the
 * `runner:common` module from [robolectric/robolectric#10897](https://github.com/robolectric/robolectric/pull/10897).
 *
 * Once Robolectric 4.15 is released and available in Maven Central, this module will provide
 * full Robolectric support with the following features:
 *
 * - Opt-in Robolectric support per test suite using TestBalloon's fixture system
 * - Configurable sandbox sharing strategies
 * - Zero impact on non-Robolectric tests
 * - TestBalloon-native API that feels natural
 *
 * ## Planned Usage
 *
 * ```kotlin
 * import de.infix.testBalloon.framework.core.testSuite
 * import de.infix.testBalloon.integration.robolectric.robolectricContext
 * import androidx.test.core.app.ApplicationProvider
 * import android.content.Context
 * import kotlin.test.assertNotNull
 *
 * val MyAndroidTests by testSuite {
 *     robolectricContext() asContextForEach {
 *         test("can use Android APIs") {
 *             val context = ApplicationProvider.getApplicationContext<Context>()
 *             assertNotNull(context)
 *         }
 *     }
 * }
 *
 * // Tests in other suites continue to run normally without Robolectric
 * val RegularJvmTests by testSuite {
 *     test("standard JVM test") {
 *         assertEquals(4, 2 + 2)
 *     }
 * }
 * ```
 *
 * ## Current Status
 *
 * This module currently serves as a template and documentation of the intended API.
 * The implementation will be completed once Robolectric 4.15 is available.
 */
@Suppress("unused")
object RobolectricIntegrationPlaceholder {
    const val REQUIRED_ROBOLECTRIC_VERSION = "4.15"
    const val ROBOLECTRIC_PR = "https://github.com/robolectric/robolectric/pull/10897"
}
