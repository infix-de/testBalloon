## Kotlin Incremental Compilation Reproducer

### How this works

The integration test depends on a local integration test repository, which contains TestBalloon's publishable artifacts. This is set up automatically under `build/integration-test-repository`.

The integration test then works as follows:

1. TestBalloon itself is used to run the tests in a Kotlin/JVM project as per [integration-test/build.gradle.kts](https://github.com/infix-de/testBalloon/blob/a619c924d3f0037a7062cbdc61c23ca5b15c2246/integration-test/build.gradle.kts#L8).
2. TestBalloon uses the test suites `incremental-compilation-testBalloon-*`.
3. For each, a project template is copied into the ephemeral project directory `integration-test/build/projects`.
4. A `testSeries()` invocation
    * uses the fixture `testTaskNames()` to query possible test tasks and prepare the project for execution (`gradlew clean`),
    * registers a series of tests:
        1. "baseline" (initial compilation from a clean state)
        2. "remove File1.kt"
        3. "restore File1.kt"
5. During its execution phase (after possible test filtering), TestBalloon runs tests in the order of registration.
6. Each test will invoke Gradle with each test task and check that results meet the expectations.

At the end, `integration-test/build/projects/incremental-compilation-testBalloon-*` are left in their final state, build reports can be examined. Also, since they are self-contained projects, they can be used for manual testing.

### Run incremental compilation for the JVM

* `./gradlew :integration-test:test --tests "IncrementalCompilationTests↘incremental-compilation-testBalloon-jvm*"`

### Run incremental compilation for KMP

* `./gradlew :integration-test:test --tests "IncrementalCompilationTests↘incremental-compilation-testBalloon-kmp*"`

### Clean all integration tests and their projects

* `./gradlew :integration-test:clean`

### Update the integration test repository (required if anything in the framework, compiler or Gradle plugin changed)

* `./gradlew :integration-test:updateIntegrationTestRepository`
