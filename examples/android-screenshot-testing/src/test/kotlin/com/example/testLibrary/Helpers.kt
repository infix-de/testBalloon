package com.example.testLibrary

import com.example.AppVersion

val versionFake = AppVersion(name = "1.0", build = "1")

const val SCREENSHOT_SDK = 34

fun referenceImageFilePath(vararg componentNames: String): String =
    "$REFERENCE_IMAGE_DIRECTORY/${componentNames.joinToString("-")}.png"

private const val REFERENCE_IMAGE_DIRECTORY = "src/test/referenceImages"
