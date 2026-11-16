plugins {
    // These plugins compile code in gradle-plugin-layer-build-logic. Their versions can differ from those
    // used to compile the project's Kotlin code elsewhere.
    kotlin("jvm") version "2.4.21"
    kotlin("plugin.sam.with.receiver") version "2.4.21"
    kotlin("plugin.assignment") version "2.4.21"
    id("java-gradle-plugin")
}

samWithReceiver {
    annotation(HasImplicitReceiver::class.qualifiedName!!)
}

assignment {
    annotation(SupportsKotlinAssignmentOverloading::class.qualifiedName!!)
}

dependencies {
    // These declarations load plugins into the root project, without applying them.
    // Doing so avoids issues when loading the same plugins in different subprojects, which will use separate
    // classloaders. In this case, plugins would not be able to communicate across projects via a shared build service.
    // Cf. https://discuss.gradle.org/t/why-duplicate-plugins-in-top-level-build-scripts/49087
    implementation(libs.org.jetbrains.kotlin.sam.with.receiver.gradle.plugin)
    implementation(libs.org.jetbrains.kotlin.assignment.gradle.plugin)
    implementation(":build-logic-base")
    compileOnly(libs.org.gradle.experimental.gradle.public.api) {
        capabilities {
            // Cf. https://github.com/gradle/gradle/issues/29483#issuecomment-2791668178
            requireCapability("org.gradle.experimental:gradle-public-api-internal")
        }
    }
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.4.21")
}

gradlePlugin {
    plugins {
        register("gradlePlugin.layer.buildLogic") {
            this.id = "gradlePlugin.layer.buildLogic"
            this.implementationClass = "gradlePlugin.layer.buildLogic.BuildLogicPlugin"
        }
    }
}
