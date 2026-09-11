import buildLogic.rootGroup

plugins {
    id("buildLogic.kotlin-jvm")
}

group = "$rootGroup.documentation"

dependencies {
    implementation(libs.org.jetbrains.dokka.core)
    implementation(libs.org.jetbrains.dokka.base)
    implementation("$rootGroup:testBalloon-framework-shared")
}
