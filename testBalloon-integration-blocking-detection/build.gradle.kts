import buildLogic.addTestBalloonPluginFromProject
import buildLogic.allTargets
import buildLogic.enableAbiValidation
import buildLogic.versionFromCatalog

plugins {
    id("buildLogic.kotlin-multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("buildLogic.publishing-multiplatform")
}

description = "Library supporting blocking code detection with the TestBalloon framework"

addTestBalloonPluginFromProject(projects.testBalloonCompilerPlugin, projects.testBalloonFrameworkShared)

kotlin {
    enableAbiValidation()

    allTargets()

    androidLibrary {
        namespace = "de.infix.testBalloon.integration.blockingDetection"
        compileSdk = versionFromCatalog("android-compileSdk").toInt()
    }

    sourceSets {
        commonMain {
            dependencies {
                api(projects.testBalloonFrameworkCore)
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test")) // for assertions only
            }
        }

        jvmMain {
            dependencies {
                implementation(libs.io.projectreactor.tools.blockhound)
                implementation(libs.org.jetbrains.kotlinx.coroutines.debug)
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    jvmArgumentProviders.add(
        CommandLineArgumentProvider {
            val javaLauncher = javaLauncher.orNull
            buildList {
                if (javaLauncher != null && javaLauncher.metadata.languageVersion >= JavaLanguageVersion.of(16)) {
                    // https://github.com/reactor/BlockHound/issues/33
                    add("-XX:+AllowRedefinitionToAddDeleteMethods")
                }
            }
        }
    )
}
