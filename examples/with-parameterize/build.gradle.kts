import buildLogic.allTargets

plugins {
    id("buildLogic.kotlin-multiplatform")
    id("de.infix.testBalloon")
}

kotlin {
    allTargets()

    sourceSets {
        commonTest {
            dependencies {
                // required for TestBalloon outside this project:
                //     implementation("de.infix.testBalloon:testBalloon-framework-core:${testBalloonVersion}")
                // instead of this project-internal dependency:
                implementation(projects.testBalloonFrameworkCore)

                implementation(libs.org.jetbrains.kotlin.test) // for assertions only
                implementation(libs.com.benwoodworth.parameterize)
            }
        }
    }
}
