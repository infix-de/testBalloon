import buildLogic.addTestBalloonPluginFromProject

plugins {
    id("buildLogic.multiplatform")
}

addTestBalloonPluginFromProject(projects.testBalloonCompilerPlugin, projects.testBalloonFrameworkAbstractions)

kotlin {
    compilerOptions {
        // freeCompilerArgs.addAll("-P", "plugin:de.infix.testBalloon:debugLevel=DISCOVERY")
    }

    sourceSets {
        commonTest {
            dependencies {
                implementation(projects.testBalloonFrameworkCore)
                implementation(kotlin("test")) // for assertions only
            }
        }
    }
}

tasks.withType(AbstractTestTask::class.java).configureEach {
    // filter.includePatterns.add("*DisplayNames*suite*1*test*1*")
    // filter.excludePatterns.add("exclude pattern")
}
