### JVM

* `gradlew :examples:multiplatform-library-with-android:cleanJvmTest :examples:multiplatform-library-with-android:jvmTest --rerun-tasks`

* `gradlew :examples:multiplatform-library-with-android:cleanJvmTest :examples:multiplatform-library-with-android:jvmTest --rerun-tasks -PtestBalloon.reportingMode=files`

* `gradlew :examples:multiplatform-library-with-android:cleanJvmTest :examples:multiplatform-library-with-android:jvmTest --tests "com.example.CommonTestsWithTestBalloon↘expected to pass*" --rerun-tasks`

* `gradlew :examples:multiplatform-library-with-android:cleanJvmTest :examples:multiplatform-library-with-android:jvmTest --tests "*to pass*" --rerun-tasks`

* `gradlew :examples:multiplatform-library-with-android:cleanJvmTest :examples:multiplatform-library-with-android:jvmTest --tests "*pass" --rerun-tasks`

### Android local

* `gradlew :examples:multiplatform-library-with-android:cleanTestAndroidHostTest :examples:multiplatform-library-with-android:testAndroidHostTest --rerun-tasks`

* `gradlew :examples:multiplatform-library-with-android:cleanTestAndroidHostTest :examples:multiplatform-library-with-android:testAndroidHostTest --rerun-tasks -PtestBalloon.reportingMode=files`

* `gradlew :examples:multiplatform-library-with-android:cleanTestAndroidHostTest :examples:multiplatform-library-with-android:testAndroidHostTest --rerun-tasks --tests "*pass"`
