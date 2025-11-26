## Kotlin Incremental Compilation Reproducer

### How this works

The integration test depends on a local integration test repository, which contains TestBalloon's publishable artifacts. This is set up automatically under `build/integration-test-repository`.

The integration test then works as follows:

1. TestBalloon itself is used to run the tests in a Kotlin/JVM project as per [integration-test/build.gradle.kts](https://github.com/infix-de/testBalloon/blob/a619c924d3f0037a7062cbdc61c23ca5b15c2246/integration-test/build.gradle.kts#L8).
2. TestBalloon uses the test suite ["incremental-compilation-testBalloon-jvm"](https://github.com/infix-de/testBalloon/blob/2f3bd678959afbe275a5a9338dc11f832cc1796b/integration-test/src/test/kotlin/IncrementalCompilationTests.kt#L37).
3. A project template is [copied into the ephemeral project directory](https://github.com/infix-de/testBalloon/blob/7494b0836994c1e814d9b66849373dc27d3f0cef/integration-test/src/test/kotlin/TestProject.kt#L27-L39) `integration-test/build/projects`.
4. [testSeries](https://github.com/infix-de/testBalloon/blob/2f3bd678959afbe275a5a9338dc11f832cc1796b/integration-test/src/test/kotlin/IncrementalCompilationTests.kt#L73)
    * uses the fixture [testTaskNames](https://github.com/infix-de/testBalloon/blob/2f3bd678959afbe275a5a9338dc11f832cc1796b/integration-test/src/test/kotlin/IncrementalCompilationTests.kt#L89) to query possible test tasks (on the JVM, it's just "test") and prepare the project for execution ("gradlew clean"),
    * registers a series of tests:
        1. "baseline" (initial compilation from a clean state)
        2. "remove File1.kt"
        3. "restore File1.kt"
5. During its execution phase (after possible test filtering), TestBalloon runs tests in the order of registration. On the JVM, each test will invoke Gradle to run the "test" task and check that results meet the expectations.

At the end, `integration-test/build/projects` is left in its final state, build reports can be examined. Also, since it is a self-contained project, it can be used for manual testing.

### How to run

* `./gradlew :integration-test:test --tests "IncrementalCompilationTests↘incremental-compilation-testBalloon-jvm*"`

### How to reset

* `./gradlew :integration-test:clean`
