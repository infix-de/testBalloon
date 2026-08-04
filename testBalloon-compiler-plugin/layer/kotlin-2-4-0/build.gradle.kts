plugins {
    id("buildLogic.kotlin-jvm-base")
}

description = "TestBalloon compiler plugin compatibility layer (2.4.0)"

dependencies {
    implementation(projects.testBalloonCompilerPlugin.base)
    implementation(projects.testBalloonCompilerPlugin.layer.kotlin2320)

    compileOnly("org.jetbrains.kotlin:kotlin-compiler:2.4.0")

    testImplementation(projects.testBalloonCompilerPlugin.baseTest)
    testImplementation(libs.org.jetbrains.kotlin.stdlib)
    testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.4.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.4.0")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
