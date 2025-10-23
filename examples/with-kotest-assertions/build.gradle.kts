import buildLogic.addTestBalloonPluginFromProject

plugins {
    id("buildLogic.multiplatform-excluding-wasm-wasi")
}

// The following invocation supplements the TestBalloon plugin declaration inside this project:
addTestBalloonPluginFromProject(projects.testBalloonCompilerPlugin, projects.testBalloonFrameworkShared)

kotlin {
    sourceSets {
        commonTest {
            dependencies {
                // required for TestBalloon outside this project:
                //     implementation("de.infix.testBalloon:testBalloon-integration-kotest-assertions:${testBalloonVersion}")
                // instead of this project-internal dependency:
                implementation(projects.testBalloonIntegrationKotestAssertions)
            }
        }
    }
}
