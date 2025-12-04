## Structured testing

TestBalloon arranges test suites and tests in the **test element hierarchy**.

![Image title](assets/coroutines/test-element-hierarchy.svg){ width="600" }
/// caption
A test element hierarchy comprised of test suites and tests. Test compartments and the test session configure the top-level test suites.
///

## Context inheritance

When tests execute, each test element (test, suite, compartment, session) has its own coroutine. The parent-child relationship between these coroutines is the same as between test elements. **Coroutine contexts are inherited** across the test element hierarchy.

!!! note

    The coroutine hierarchy mirrors the test element hierarchy.

## Suspending code

During execution, code can suspend in ([:material-check-circle:{ .green } green code](tests-and-suites.md#green-code-and-blue-code) of)

* tests,
* test fixtures,
* execution wrappers.

!!! info

    :testballoon: Blue code inside a test suite (which registers tests, suites, fixtures) cannot suspend. Registration is sequential. This enables consistent multiplatform integration, as lower-level test infrastructures require it.

## Deep concurrency and parallelism

TestBalloon can be [configured](configuration.md#testconfig) to execute tests concurrently or in parallel(1) at any level of the test hierarchy. This means that it can run all tests concurrently, or run selected tests concurrently and others sequentially.
{ .annotate }

1. Parallel execution requires a multithreaded platform like the JVM, Native, or Android.

Wherever TestBalloon runs tests concurrently, it does so with **deep concurrency** by default: It uses a common coroutine dispatcher to govern concurrency across the chosen parts of the test element hierarchy. This optimally distributes load across CPU cores.

!!! note

    Contrast this with _shallow concurrency_, which parallelizes just the top-level, or complex parallelization schemes configured at different levels. These approaches create concurrency bottlenecks, leave CPUs underutilized, and thus cannot provide comparable throughput.

!!! warning

    Never use the `maxParallelForks` option on Gradle test tasks. Gradle has no idea about the test structure and assumes class-based tests, which TestBalloon does not use.

## TestScope by default

All tests use kotlinx.coroutines' [TestScope] by default, including its virtual time and delay-skipping. A property `testScope` provides access to this `TestScope`.

!!! info

    `TestConfig.testScope` can configure the presence of a `TestScope` (and its timeout) for all or part of a test element hierarchy. You can always choose to execute your tests on a standard or custom dispatcher, and with real-time behavior.

!!! warning

    `TestScope` uses `runBlocking` internally, which is incompatible with concurrent execution on a dispatcher using a limited number of threads. The combination can [cause hangups due to thread starvation](https://github.com/Kotlin/kotlinx.coroutines/issues/4579).

[TestScope]: https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/kotlinx.coroutines.test/-test-scope.html
