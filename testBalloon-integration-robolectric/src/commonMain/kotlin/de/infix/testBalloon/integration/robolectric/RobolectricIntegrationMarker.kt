package de.infix.testBalloon.integration.robolectric

/**
 * Marker file for commonMain source set.
 *
 * The Robolectric integration is JVM-only, as Robolectric itself is a JVM library.
 * All actual implementation is in the jvmMain source set.
 *
 * This file ensures the commonMain source set is valid for the multiplatform configuration.
 */
internal object RobolectricIntegrationMarker
