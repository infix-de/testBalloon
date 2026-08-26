plugins {
    id("buildLogic.kotlin-jvm")
}

dependencies {
    implementation(libs.org.jetbrains.dokka.core)
    implementation(libs.org.jetbrains.dokka.base)
    implementation("$group:testBalloon-framework-shared:$version")
}
