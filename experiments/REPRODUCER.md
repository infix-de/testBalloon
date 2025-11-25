### REPRODUCER

Incremental compilation in Kotlin 2.3.20-dev-6091

#### JVM works

1. `./gradlew clean`

2. `./gradlew jvmTest --no-build-cache`

3. Change the name of `test("min")` in `experiments/src/commonTest/kotlin/com/example/ExperimentalSuite.kt`.

4. `./gradlew jvmTest --no-build-cache`

#### JS without incremental compilation: works

1. `./gradlew clean`

2. `./gradlew jsNodeTest --no-build-cache`

3. Change the name of `test("min")` in `experiments/src/commonTest/kotlin/com/example/ExperimentalSuite.kt`.

4. `./gradlew jsNodeTest --no-build-cache`

#### JS with incremental compilation: "No file found for source null", "KT-82395"

1. `./gradlew clean`

2. `./gradlew jsNodeTest -PtestBalloon.nonIncrementalTestCompileTaskRegex=DoNotMatch --no-build-cache`

    ```
    e: java.lang.IllegalStateException: No file found for source null
    This happened because there is a compiler plugin which generates new top-level declarations
    and the incremental compilation is enabled.
    Consider disabling the incremental compilation for this module or disable the plugin.
    If you met this error, please describe your use-case in https://youtrack.jetbrains.com/issue/KT-82395
        at org.jetbrains.kotlin.backend.common.serialization.SerializeModuleIntoKlibKt.serializeModuleIntoKlib$lambda$2$0(serializeModuleIntoKlib.kt:146)
        at org.jetbrains.kotlin.fir.pipeline.Fir2KlibMetadataSerializer.forEachFile(Fir2KlibMetadataSerializer.kt:100)
    ```

#### Native works (but the kotlin build reports suggest that incremental compilation is unavailable)

1. `./gradlew clean`

2. `./gradlew linuxX64Test --no-build-cache`

3. Change the name of `test("min")` in `experiments/src/commonTest/kotlin/com/example/ExperimentalSuite.kt`.

4. `./gradlew linuxX64Test --no-build-cache`
