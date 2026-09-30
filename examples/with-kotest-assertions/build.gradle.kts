import buildLogic.allTargets
import buildLogic.kotestJdkVersion

plugins {
    id("buildLogic.kotlin-multiplatform")
    id("de.infix.testBalloon")
}

tapmoc {
    java(kotestJdkVersion())
}

kotlin {
    allTargets()

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
