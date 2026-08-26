plugins {
    id("buildLogic.kotlin-jvm")
    id("de.infix.testBalloon")
}

dependencies {
    // required for TestBalloon outside this project:
    //     testImplementation("de.infix.testBalloon:testBalloon-framework-core:${testBalloonVersion}")
    // instead of this project-internal dependency:
    testImplementation(projects.testBalloonFrameworkCore)

    testImplementation(libs.org.jetbrains.kotlin.test) // for assertions only
}
