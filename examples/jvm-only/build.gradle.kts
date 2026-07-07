import buildLogic.addTestBalloonPluginFromProject

plugins {
    id("org.jetbrains.kotlin.jvm")
    // id("de.infix.testBalloon") version "$testBalloonVersion"  // required for TestBalloon outside this project
}

// The following invocation supplements the TestBalloon plugin declaration inside this project:
addTestBalloonPluginFromProject(projects.testBalloonCompilerPlugin)

dependencies {
    // required for TestBalloon outside this project:
    //     testImplementation("de.infix.testBalloon:testBalloon-framework-core:${testBalloonVersion}")
    // instead of this project-internal dependency:
    testImplementation(projects.testBalloonFrameworkCore)

    testImplementation(libs.org.jetbrains.kotlin.test) // for assertions only
}
