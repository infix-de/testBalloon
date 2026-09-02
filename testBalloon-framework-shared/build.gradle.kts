import buildLogic.allTargets
import buildLogic.enableAbiValidation

plugins {
    id("buildLogic.kotlin-multiplatform")
    id("buildLogic.publishing")
}

description = "Shared declarations for the TestBalloon framework"

kotlin {
    enableAbiValidation()

    allTargets()

    sourceSets {
        jvmTest {
            dependencies {
                implementation(libs.org.jetbrains.kotlin.test)
                implementation(libs.io.kotest.assertions.core)
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
