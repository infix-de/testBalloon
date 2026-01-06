import buildLogic.addTestBalloonPluginFromProject

plugins {
    id("org.jetbrains.kotlin.jvm")
    java
    `jvm-test-suite`
    // id("de.infix.testBalloon") version "$testBalloonVersion"  // required for TestBalloon outside this project
}

// The following invocation supplements the TestBalloon plugin declaration inside this project:
addTestBalloonPluginFromProject(projects.testBalloonCompilerPlugin, projects.testBalloonFrameworkShared)

@Suppress("UnstableApiUsage")
testing {
    suites {
        val test by getting(JvmTestSuite::class) {
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
