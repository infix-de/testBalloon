plugins {
    id("buildLogic.kotlin-jvm-base")
}

description = "TestBalloon compiler plugin compatibility layer (2.3.0)"

dependencies {
    implementation(projects.testBalloonCompilerPlugin.base)
    compileOnly("org.jetbrains.kotlin:kotlin-compiler:2.3.0")

    testImplementation(projects.testBalloonCompilerPlugin.baseTest)

    testImplementation(libs.org.jetbrains.kotlin.stdlib)
    testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.3.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.3.0")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
