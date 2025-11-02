### Run Tests

* `./gradlew clean`

#### JVM

* `./gradlew :cleanJvmTest :jvmTest -PtestBalloon.environmentVariables=FROM_PROPERTY`

#### JS/Node

* `./gradlew :cleanJsNodeTest :jsNodeTest -PtestBalloon.environmentVariables=FROM_PROPERTY`

#### JS/Browser

* `./gradlew :cleanJsBrowserTest :jsBrowserTest -PtestBalloon.environmentVariables=FROM_PROPERTY`
