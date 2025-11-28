### TRIAL

* `./gradlew clean`

### Debugging the compiler plugin

Attach the JVM debugger to port 5005. In IntelliJ IDEA, use the command Run – Attach to Process.

Note: Incremental compilation is disabled with "in-process" execution strategy.

### JS

* `./gradlew -Pkotlin.compiler.execution.strategy=in-process clean jsNodeTest --no-build-cache`

* `./gradlew -Dorg.gradle.debug=true -Pkotlin.compiler.execution.strategy=in-process jsNodeTest`

* `./gradlew -Pkotlin.compiler.execution.strategy=in-process jsNodeTest`

* `./gradlew jsNodeTest`

* `./gradlew -Dorg.gradle.debug=true -Pkotlin.compiler.execution.strategy=in-process clean compileTestKotlinJs --no-build-cache`

### Native

* `./gradlew -Pkotlin.compiler.execution.strategy=in-process clean linuxX64Test --no-build-cache`

* `./gradlew -Pkotlin.compiler.execution.strategy=in-process linuxX64Test`

* `./gradlew linuxX64Test`

1. `./gradlew -Dorg.gradle.debug=true -Pkotlin.compiler.execution.strategy=in-process clean compileTestKotlinLinuxX64 --no-build-cache`

### JVM

* `./gradlew -Pkotlin.compiler.execution.strategy=in-process clean jvmTest --no-build-cache -Pkotlin.incremental=false -Pkotlin.build.report.output=file -Pkotlin.build.report.enable=true -Pkotlin.build.report.verbose=true`

* `./gradlew -Pkotlin.incremental=false jvmTest`

* `./gradlew -Pkotlin.compiler.execution.strategy=in-process jvmTest`

* `./gradlew jvmTest`

* `./gradlew -Dorg.gradle.debug=true -Pkotlin.compiler.execution.strategy=in-process clean compileTestKotlinJvm --no-build-cache`
