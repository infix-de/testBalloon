import buildLogic.addTestBalloonPluginFromProject

plugins {
    id("org.jetbrains.kotlin.jvm")
    // id("de.infix.testBalloon") version "$testBalloonVersion"  // required for TestBalloon outside this project
}

// The following invocation supplements the TestBalloon plugin declaration inside this project:
addTestBalloonPluginFromProject(projects.testBalloonCompilerPlugin, projects.testBalloonFrameworkShared)

dependencies {
    // required for TestBalloon outside this project:
    //     implementation("de.infix.testBalloon:testBalloon-framework-core:${testBalloonVersion}")
    // instead of this project-internal dependency:
    implementation(projects.testBalloonFrameworkCore)

    implementation(kotlin("test")) // for assertions only
}
