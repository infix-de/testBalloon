@file:OptIn(TestBalloonInternalApi::class)

package de.infix.testBalloon.compilerPlugin

import de.infix.testBalloon.framework.shared.internal.Constants
import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalApi
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

val entryPointPackageFqName = FqName(Constants.ENTRY_POINT_PACKAGE_NAME)

val mainCallableId = CallableId(
    packageName = entryPointPackageFqName,
    callableName = Name.identifier("main")
)

val entryPointPropertyCallableId = CallableId(
    packageName = entryPointPackageFqName,
    callableName = Name.identifier("testFrameworkEntryPoint")
)
