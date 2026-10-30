### Checking the `kotlin-compiler-plugin-api` artifact 

This Gradle Included Build compiles a **self-contained compiler plugin** against the `kotlin-compiler-plugin-api` artifact.

#### Why this plugin layer differs from the others

Usually, TestBalloon has a multi-version compiler plugin layer structure, providing a compiler plugin variant per Kotlin compiler API "stepping". Therein, newer plugin layers build on older plugin layers, changing only what's needed. This plugin layer is different in that it is self-contained. It does not depend on older layers, whose code has just been copy-pasted. In effect, the entire plugin code is compiled against the `kotlin-compiler-plugin-api` artifact (not just the latest layer changes).

#### What needs to change when productizing this module

In principle, this module could become a new base layer for all plugin layers introduced after the `kotlin-compiler-plugin-api` artifact is released. In needs to be investigated if the duplication of code, which would increase the effort required for cross-layer changes, outweighs the benefits of having a compile-time verification for every change against the API artifact.

> If this compiles, its version still must be added to `CompilerPluginRegistrar.compilerAdapter`.

#### Compiling, running tests

* `./gradlew :testBalloon-compiler-plugin:kotlin-2-5-20:compileKotlin`
* `./gradlew :testBalloon-compiler-plugin:kotlin-2-5-20:test`

#### Results for Kotlin 2.5.20-dev-2623

``` 
> Task :testBalloon-compiler-plugin:kotlin-2-5-20:compileKotlin FAILED
e: .../testBalloon/testBalloon-compiler-plugin/layer/kotlin-2-5-20/src/main/kotlin/de/infix/testBalloon/compilerPlugin/layer/kotlin2520/CompilerAdapter_2_5_20.kt:9:37 Unresolved reference 'jvm'.
e: .../testBalloon/testBalloon-compiler-plugin/layer/kotlin-2-5-20/src/main/kotlin/de/infix/testBalloon/compilerPlugin/layer/kotlin2520/CompilerAdapter_2_5_20.kt:63:40 Unresolved reference 'fileParent' on receiver of type 'IrDeclarationWithName'.
e: .../testBalloon/testBalloon-compiler-plugin/layer/kotlin-2-5-20/src/main/kotlin/de/infix/testBalloon/compilerPlugin/layer/kotlin2520/FirDeclarationGenerationExtensionBase.kt:30:38 Unresolved reference 'jvm'.
e: .../testBalloon/testBalloon-compiler-plugin/layer/kotlin-2-5-20/src/main/kotlin/de/infix/testBalloon/compilerPlugin/layer/kotlin2520/FirDeclarationGenerationExtensionBase.kt:31:38 Unresolved reference 'konan'.
e: .../testBalloon/testBalloon-compiler-plugin/layer/kotlin-2-5-20/src/main/kotlin/de/infix/testBalloon/compilerPlugin/layer/kotlin2520/FirDeclarationGenerationExtensionBase.kt:57:22 Unresolved reference 'isJvm' on receiver of type 'TargetPlatform'.
e: .../testBalloon/testBalloon-compiler-plugin/layer/kotlin-2-5-20/src/main/kotlin/de/infix/testBalloon/compilerPlugin/layer/kotlin2520/FirDeclarationGenerationExtensionBase.kt:71:22 Unresolved reference 'isJvm' on receiver of type 'TargetPlatform'.
e: .../testBalloon/testBalloon-compiler-plugin/layer/kotlin-2-5-20/src/main/kotlin/de/infix/testBalloon/compilerPlugin/layer/kotlin2520/IrGenerationExtensionBase.kt:26:37 Unresolved reference 'jvm'.
e: .../testBalloon/testBalloon-compiler-plugin/layer/kotlin-2-5-20/src/main/kotlin/de/infix/testBalloon/compilerPlugin/layer/kotlin2520/IrGenerationExtensionBase.kt:106:38 Unresolved reference 'jvm'.
e: .../testBalloon/testBalloon-compiler-plugin/layer/kotlin-2-5-20/src/main/kotlin/de/infix/testBalloon/compilerPlugin/layer/kotlin2520/IrGenerationExtensionBase.kt:326:26 Unresolved reference 'isJvm' on receiver of type 'TargetPlatform?'.
e: .../testBalloon/testBalloon-compiler-plugin/layer/kotlin-2-5-20/src/main/kotlin/de/infix/testBalloon/compilerPlugin/layer/kotlin2520/IrGenerationExtensionBase.kt:327:61 Unresolved reference 'fileParent' on receiver of type 'IrDeclaration'.
e: .../testBalloon/testBalloon-compiler-plugin/layer/kotlin-2-5-20/src/main/kotlin/de/infix/testBalloon/compilerPlugin/layer/kotlin2520/IrGenerationExtensionBase.kt:332:62 Unresolved reference 'fileParent' on receiver of type 'IrSimpleFunction'.
e: .../testBalloon/testBalloon-compiler-plugin/layer/kotlin-2-5-20/src/main/kotlin/de/infix/testBalloon/compilerPlugin/layer/kotlin2520/IrGenerationExtensionBase.kt:336:74 Unresolved reference 'fileParent' on receiver of type 'IrProperty'.
```
