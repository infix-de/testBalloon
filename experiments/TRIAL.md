### TRIAL

* `./gradlew clean`

### IDE

* `./gradlew --no-build-cache cleanAllTests allTests`
* `./gradlew --no-build-cache cleanJvmTest jvmTest`
* `./gradlew --no-build-cache cleanJsBrowserTest jsBrowserTest`
* `./gradlew --no-build-cache cleanPixel2api30AndroidDeviceTest pixel2api30AndroidDeviceTest`
* `./gradlew --no-build-cache cleanTestAndroidHostTest testAndroidHostTest`

### HTML

* `./gradlew -PtestBalloon.reportingMode=files --no-build-cache cleanAllTests allTests`
* `./gradlew -PtestBalloon.reportingMode=files --no-build-cache cleanJvmTest jvmTest`
* `./gradlew -PtestBalloon.reportingMode=files --no-build-cache cleanJsNodeTest jsNodeTest`
* `./gradlew -PtestBalloon.reportingMode=files --no-build-cache cleanPixel2api30AndroidDeviceTest pixel2api30AndroidDeviceTest`
* `./gradlew -PtestBalloon.reportingMode=files --no-build-cache cleanTestAndroidHostTest testAndroidHostTest`
