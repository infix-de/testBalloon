import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension

@Suppress("unused")
class BuildLogicPublishingBasePlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        with(pluginManager) {
            apply("signing")
            apply("com.vanniktech.maven.publish.base")
        }

        extensions.configure<MavenPublishBaseExtension>("mavenPublishing") {
            publishToMavenCentral()

            signAllPublications()

            pom {
                name.set(project.name)
                description.set(
                    provider {
                        checkNotNull(project.description) {
                            "Project description must be set for project '${project.path}'"
                        }
                    }
                )

                url.set("https://github.com/infix-de/testBalloon/")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        id.set("infix-de")
                        name.set("infix Software")
                        url.set("https://github.com/infix-de")
                    }
                }

                scm {
                    url.set("https://github.com/infix-de/testBalloon/")
                    connection.set("scm:git:git://github.com/infix-de/testBalloon.git")
                    developerConnection.set("scm:git:ssh://git@github.com/infix-de/testBalloon.git")
                }
            }
        }

        extensions.configure<PublishingExtension>("publishing") {
            repositories {
                System.getProperty("user.home")?.let { home ->
                    maven {
                        name = "local"
                        url = uri("$home/.m2/local-repository")
                    }
                }

                maven {
                    name = "integrationTest"
                    url = uri(rootProject.layout.buildDirectory.dir("integration-test-repository"))
                }
            }
        }

        extensions.configure<SigningExtension>("signing") {
            isRequired = false // not necessary for local publishing
        }

        // WORKAROUND https://github.com/gradle/gradle/issues/26091
        //     "Task ':testBalloon-gradle-plugin:publishPluginMavenPublicationToMavenCentralRepository' uses this output of task ':testBalloon-gradle-plugin:signMavenPublication' without declaring an explicit or implicit dependency."
        //     Caused by a single source jar or dokka jar artifact being added to all publications.
        //     See https://github.com/gradle/gradle/issues/26091#issuecomment-1836156762
        tasks.withType(AbstractPublishToMaven::class.java).configureEach {
            val signingTasks = tasks.withType(Sign::class.java)
            mustRunAfter(signingTasks)
        }

        // WORKAROUND https://github.com/gradle/gradle/issues/26132
        //     "Task ':linkDebugTestLinuxX64' uses this output of task ':signLinuxX64Publication' without declaring an explicit or implicit dependency."
        //     Signing plugin generates overlapping task outputs
        afterEvaluate {
            tasks.configureEach {
                if (name.startsWith("compileTestKotlin")) {
                    val target = name.substring("compileTestKotlin".length)
                    val sign = try {
                        tasks.named("sign${target}Publication")
                    } catch (e: Throwable) {
                        null
                    }
                    if (sign != null) {
                        dependsOn(sign)
                    }
                }
            }
        }
    }
}
