plugins {
    kotlin("jvm") version "{{version:org.jetbrains.kotlin}}"
    id("com.gradleup.tapmoc") version "{{version:com.gradleup.tapmoc}}"
    id("de.infix.testBalloon") version "{{version:de.infix.testBalloon}}"
}

tapmoc {
    java("{{version:base.jdk}}".toInt())
    kotlin("{{version:org.jetbrains.kotlin}}")
}

dependencies {
    implementation("de.infix.testBalloon:testBalloon-framework-core:{{version:de.infix.testBalloon}}")
}

tasks {
    register("listTests") {
        group = "verification"

        doLast {
            println("##TEST(test)##")
        }
    }

    withType<Test>().configureEach {
        testLogging { showStandardStreams = true }
    }
}
