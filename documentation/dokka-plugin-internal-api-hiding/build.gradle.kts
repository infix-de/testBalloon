import buildLogic.rootGroup
import tapmoc.Severity

plugins {
    id("buildLogic.kotlin-jvm")
}

group = "$rootGroup.documentation"

tapmoc {
    checkDependencies(Severity.IGNORE) // Neutralize the defaults for this internal component.
}

dependencies {
    implementation(libs.org.jetbrains.dokka.core)
    implementation(libs.org.jetbrains.dokka.base)
    implementation("$rootGroup:testBalloon-framework-shared")
}
