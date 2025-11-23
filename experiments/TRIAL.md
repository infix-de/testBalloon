### TRIAL

* `./gradlew clean`

### IDE

* `./gradlew --rerun-tasks cleanAllTests allTests`
* `./gradlew --rerun-tasks cleanJvmTest jvmTest`
* `./gradlew --rerun-tasks cleanJsBrowserTest jsBrowserTest`
* `./gradlew --rerun-tasks cleanPixel2api30AndroidDeviceTest pixel2api30AndroidDeviceTest`
* `./gradlew --rerun-tasks cleanTestAndroidHostTest testAndroidHostTest`

### HTML

* `./gradlew -PtestBalloon.reportingMode=files --rerun-tasks cleanAllTests allTests`
* `./gradlew -PtestBalloon.reportingMode=files --rerun-tasks cleanJvmTest jvmTest`
* `./gradlew -PtestBalloon.reportingMode=files --rerun-tasks cleanJsNodeTest jsNodeTest`
* `./gradlew -PtestBalloon.reportingMode=files --rerun-tasks cleanPixel2api30AndroidDeviceTest pixel2api30AndroidDeviceTest`
* `./gradlew -PtestBalloon.reportingMode=files --rerun-tasks cleanTestAndroidHostTest testAndroidHostTest`
