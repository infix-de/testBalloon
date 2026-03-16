> _I didn't know Kotlin testing could be that easy._

## Kotlin first

DSL-based and powered by a compiler plugin, TestBalloon **eliminates bloat** from test code.(1)
{ .annotate }

1. Find examples in the documentation's sections on [Migrating from JUnit](../how-to/migrating-from-junit.md#extensions) and other test frameworks.

With its concise DSL and decorator chains for configuration, TestBalloon is **so easy to customize that it's actually yours**.

## Perfectly integrated

TestBalloon is **compatible with existing assertion libraries** and Kotlin Power Assert.

TestBalloon **integrates thoroughly** with the platforms' existing APIs and build tooling, using the familiar Gradle tasks and Kotlin's own platform-specific test runtimes.

It supports **all Kotlin target platforms(1) in first-party quality** (including Android device tests and Robolectric). TestBalloon has full coroutine support built in.
{ .annotate }

1. TestBalloon supports all Kotlin target platforms (JVM, JS, WebAssembly, Android host-side tests, Android device-side tests, Linux, Windows, iOS, macOS and other Apple targets).

## Capable

TestBalloon provides **easy access to the most advanced capabilities** in Kotlin testing:

- [x] Parameterized tests
- [x] Multi-level hierarchy
- [x] Coroutines
- [x] Coroutine context inheritance
- [x] Deep parallelism
- [x] Fixtures
- [x] Expressive names
- [x] Scope-friendly DSL
- [x] Configuration via decorator chains

## Empowering

TestBalloon helps you **lift product quality**, save time and actually **make testing enjoyable**.

- [x] Write *better tests* (parameterize them)
- [x] with *less effort* (using your Kotlin skills and a simple API)
- [x] on *all platforms* (with native integration that just works).

What else?

- [x] Stay *organized* (with expressive names and a multi-level hierarchy),
- [x] get *faster results* (with deep parallelism in test runs),
- [x] remain *compatible* (with all Kotlin releases since 2.0).

## Robust

TestBalloon consists of production-quality code and has been **intensively tested**. Its own test suite runs all component tests on all Kotlin targets, plus integration tests on all Kotlin target categories.

## Fast

TestBalloon **can cover large test sets** and has been observed running **1.7 million real-world tests in 86 seconds** on a Framework 13 Laptop (18 cores, 4 GB JVM heap) with concurrent execution enabled.

## Production-ready

TestBalloon is **actively used in production**.

Open source products using TestBalloon include:

- [x] [Prepared](https://prepared.opensavvy.dev/index.html) – a Kotlin Multiplatform test library featuring time management, parameterization and isolated fixtures
- [x] [Signum](https://github.com/a-sit-plus/signum) – a Kotlin Multiplatform crypto/PKI library and ASN1 parser + encoder
- [x] [VC-K](https://github.com/a-sit-plus/vck) – a verifiable credentials library for Kotlin Multiplatform
- [x] [Warden Supreme](https://github.com/a-sit-plus/warden-supreme) – an integrated key and app attestation suite

Of course, advanced testing is often found in closed-source products, and TestBalloon is in active use there as well.

### Stability

TestBalloon has a **stable feature set**. However, it is still **evolving**, so you should expect some migration issues. Breaking changes will be documented in the release notes, along with migration guidance.

## Why wait?

TestBalloon is your trouble reducer and helps you **release with confidence**.

- [x] Powerful multiplatform testing made easy.
- [x] Test more (edge) cases with less effort.
- [x] Keep your AI-generated code in check.
