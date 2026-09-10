plugins {
    id("buildLogic.kotlin-jvm")
}

val rootGroup = "${project.property("local.PROJECT_GROUP_ID")}"
group = "$rootGroup.documentation"

dependencies {
    implementation(libs.org.jetbrains.dokka.core)
    implementation(libs.org.jetbrains.dokka.base)
    implementation("$rootGroup:testBalloon-framework-shared:$version")
}
