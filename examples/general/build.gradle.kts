import buildLogic.allTargets

plugins {
    id("buildLogic.kotlin-multiplatform")
    id("de.infix.testBalloon")
    id("org.jetbrains.kotlin.plugin.atomicfu")
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
                implementation(libs.org.jetbrains.kotlinx.atomicfu)
            }
        }

        jvmTest {
            dependencies {
                implementation(libs.org.jetbrains.kotlinx.coroutines.debug)
            }
        }
    }
}
