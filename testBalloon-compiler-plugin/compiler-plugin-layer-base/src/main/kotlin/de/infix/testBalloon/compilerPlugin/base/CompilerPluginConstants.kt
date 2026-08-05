@file:OptIn(TestBalloonInternalApi::class)

package de.infix.testBalloon.compilerPlugin.base

import buildConfig.BuildConfig.PROJECT_COMPILER_PLUGIN_ID
import de.infix.testBalloon.framework.shared.internal.Constants
import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalApi
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

val entryPointPackageFqName = FqName(Constants.ENTRY_POINT_PACKAGE_NAME)

val mainFunctionId = CallableId(
    packageName = entryPointPackageFqName,
    callableName = Name.identifier("main")
)

val nativeEntryPointPropertyId = CallableId(
    packageName = entryPointPackageFqName,
    callableName = Name.identifier("testFrameworkNativeEntryPoint")
)

const val PLUGIN_ID = PROJECT_COMPILER_PLUGIN_ID

const val PLUGIN_DISPLAY_NAME = "Plugin $PLUGIN_ID"
