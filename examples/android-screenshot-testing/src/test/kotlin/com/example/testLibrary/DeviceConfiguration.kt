package com.example.testLibrary

/**
 * A device display and settings configuration.
 */
enum class DeviceConfiguration(val qualifiers: String, val fontScale: Float = 1.0f, val isDark: Boolean = false) {
    BASELINE(qualifiers = "+$BASELINE_LOCALE-$PHONE_SIZE"),

    /** Catches missing translations and unsuitable layouts. */
    LONGEST_TRANSLATIONS(qualifiers = "+$LONGEST_TRANSLATIONS_LOCALE-$PHONE_SIZE"),

    /** Catches hardcoded colors. */
    DARK(qualifiers = "+$BASELINE_LOCALE-$PHONE_SIZE-night", isDark = true),

    /** Catches layouts that assume phone width. */
    TABLET(qualifiers = "+$BASELINE_LOCALE-$TABLET_SIZE"),

    /** Catches truncation and overlap. */
    LARGEST_FONT_SCALE(
        qualifiers = "+$LONGEST_TRANSLATIONS_LOCALE-$PHONE_SIZE",
        // The largest possible font scale:
        // The display-settings slider stops at "huge" (1.3), but the accessibility
        // font-size setting goes to 2.0, which can actually break layouts.
        fontScale = 2.0f
    );

    val displayName = name.lowercase().replace('_', ' ')
}

private const val BASELINE_LOCALE = "en"

/** The locale whose translations consume the most horizontal space. */
private const val LONGEST_TRANSLATIONS_LOCALE = "fr"

/** A modern reference phone, matching AndroidX's `Devices.PHONE` preview spec. */
private const val PHONE_SIZE = "w411dp-h891dp"

/** A tablet-sized portrait display. */
private const val TABLET_SIZE = "w800dp-h1280dp"
