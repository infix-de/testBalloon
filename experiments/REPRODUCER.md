### REPRODUCER

Incremental compilation in Kotlin 2.3.20-dev-* with FIR-generated top-level symbols

#### JVM "Expected absolute path but found relative path"

1. In `testBalloon-compiler-plugin/src/main/kotlin/de/infix/testBalloon/compilerPlugin/CompilerPluginIrGenerationExtension.kt`,
search for `val breakHere`, change the line to `val breakHere = true`.

2. `./gradlew clean`

3. `./gradlew jvmTest --no-build-cache -Pkotlin.incremental=false`

    ```
    > There are test sources present and no filters are applied
    ```

4. `./gradlew jvmTest --no-build-cache`

    ```
    Suppressed: java.lang.Exception: Could not close incremental caches in /.../testBalloon/experiments/build/kotlin/compileTestKotlinJvm/cacheable/caches-jvm/inputs: source-to-output.tab
        ... 25 more
        Suppressed: java.lang.IllegalStateException: Expected absolute path but found relative path: de/infix/testBalloon/framework/shared/internal/entryPoint/__GENERATED__CALLABLES__.kt
            at org.jetbrains.kotlin.incremental.storage.RelocatableFileToPathConverter.toPath(RelocatableFileToPathConverter.kt:20)
    ```

#### JVM "Could not close incremental caches"

1. In `testBalloon-compiler-plugin/src/main/kotlin/de/infix/testBalloon/compilerPlugin/CompilerPluginIrGenerationExtension.kt`,
   search for `val breakHere`, change the line to `val breakHere = false`.

2. `./gradlew clean`

3. `./gradlew jvmTest --no-build-cache -Pkotlin.incremental=false`

    ```
    BUILD SUCCESSFUL
    ```

4. `./gradlew jvmTest --no-build-cache`

    ```
    Caused by: java.lang.Exception: Could not close incremental caches in /.../testBalloon/experiments/build/kotlin/compileTestKotlinJvm/cacheable/caches-jvm/jvm/kotlin: source-to-classes.tab, internal-name-to-source.tab
        at org.jetbrains.kotlin.incremental.storage.BasicMapsOwner.forEachMapSafe(BasicMapsOwner.kt:95)
        at org.jetbrains.kotlin.incremental.storage.BasicMapsOwner.close(BasicMapsOwner.kt:53)
        at org.jetbrains.kotlin.com.google.common.io.Closer.close(Closer.java:205)
        ... 22 more
        Suppressed: java.lang.IllegalStateException: Storage for [/.../testBalloon/experiments/build/kotlin/compileTestKotlinJvm/cacheable/caches-jvm/jvm/kotlin/source-to-classes.tab] is already registered
            at org.jetbrains.kotlin.com.intellij.util.io.FilePageCache.registerPagedFileStorage(FilePageCache.java:410)
    ```

#### JS "No file found for source null", "KT-82395"

1. `./gradlew clean`

2. `./gradlew jsNodeTest -Pkotlin.incremental=false -Pkotlin.incremental.js=false -Pkotlin.incremental.js.klib=false -Pkotlin.incremental.js.ir=false --no-build-cache`

    ```
    BUILD SUCCESSFUL
    ```

3. `./gradlew jsNodeTest --no-build-cache`

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

2. `./gradlew linuxX64Test -Pkotlin.incremental=false --no-build-cache`

    ```
    BUILD SUCCESSFUL
    ```

3. `./gradlew linuxX64Test --no-build-cache`

    ```
    > Task :experiments:linuxX64Test UP-TO-DATE
    [...]
    BUILD SUCCESSFUL in 4s
    ```

4. Change the name of `test("min")` in `experiments/src/commonTest/kotlin/com/example/ExperimentalSuite.kt`.

5. `./gradlew linuxX64Test`

    ```
    BUILD SUCCESSFUL
    ```
