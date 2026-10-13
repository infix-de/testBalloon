import buildLogic.rootGroup
import com.gradleup.tapmoc.Severity

plugins {
    id("buildLogic.kotlin-jvm")
}

group = "$rootGroup.documentation"

// Neutralize the defaults for this internal component.
buildLogicCommon.javaClassFilesSeverity.set(Severity.IGNORE)
buildLogicCommon.kotlinMetadataSeverity.set(Severity.IGNORE)
buildLogicCommon.kotlinStdlibSeverity.set(Severity.IGNORE)

dependencies {
    implementation(libs.org.jetbrains.dokka.core)
    implementation(libs.org.jetbrains.dokka.base)
}
