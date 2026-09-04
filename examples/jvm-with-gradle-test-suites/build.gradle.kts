plugins {
    id("buildLogic.kotlin-jvm")
    id("de.infix.testBalloon")
    java
}

@Suppress("UnstableApiUsage")
testing {
    suites {
        val test = named<JvmTestSuite>("test") {
            dependencies {
                // required for TestBalloon outside this project:
                //     implementation("de.infix.testBalloon:testBalloon-framework-core:${testBalloonVersion}")
                // instead of this project-internal dependency:
                implementation(projects.testBalloonFrameworkCore)
                implementation(libs.org.jetbrains.kotlin.test) // for assertions only
            }
        }

        register<JvmTestSuite>("integrationTest") {
            dependencies {
                // required for TestBalloon outside this project:
                //     implementation("de.infix.testBalloon:testBalloon-framework-core:${testBalloonVersion}")
                // instead of this project-internal dependency:
                implementation(projects.testBalloonFrameworkCore)
                implementation(libs.org.jetbrains.kotlin.test) // for assertions only
            }
        }

        register<JvmTestSuite>("anotherGradleSuite") {
            dependencies {
                // required for TestBalloon outside this project:
                //     implementation("de.infix.testBalloon:testBalloon-framework-core:${testBalloonVersion}")
                // instead of this project-internal dependency:
                implementation(projects.testBalloonFrameworkCore)
                implementation(libs.org.jetbrains.kotlin.test) // for assertions only
            }
            targets {
                all {
                    testTask.configure {
                        useJUnitPlatform()
                    }
                }
            }
        }
    }
}
