## The Android Host Test Dance

### Why Dance?

The KMP+Android Gradle plugin combination appears broken (tested up to AGP 8.13.0).

You will see the message from the `compileAndroidHostTest` task:

```
Task :examples:multiplatform-library-with-android:compileAndroidHostTest FAILED
e: Plugin de.infix.testBalloon: Could not find function 'de.infix.testBalloon.framework.internal.configureAndExecuteTests'.
Please add the dependency 'de.infix.testBalloon:testBalloon-framework-core:0.6.2-K2.2.20-SNAPSHOT'.
```

### How To Dance

1. `gradlew --no-build-cache clean testAndroidHostTest`
   > Plugin de.infix.testBalloon: Could not find function 'de.infix.testBalloon.framework.internal.configureAndExecuteTests'.
2. `gradlew --no-build-cache clean testAndroidHostTest -Plocal.androidHostTestDance=removeDependency`
   > [OK] There are test sources present and no filters are applied, but the test task did not discover any tests to execute.
3. `gradlew --no-build-cache testAndroidHostTest`
   > [OK] 8 tests completed, 4 failed
