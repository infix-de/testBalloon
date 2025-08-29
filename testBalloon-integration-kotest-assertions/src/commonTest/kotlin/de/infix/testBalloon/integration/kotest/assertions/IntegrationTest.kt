package de.infix.testBalloon.integration.kotest.assertions

import de.infix.testBalloon.framework.TestBalloonExperimentalApi
import de.infix.testBalloon.framework.TestConfig
import de.infix.testBalloon.framework.TestPlatform
import de.infix.testBalloon.framework.testPlatform
import de.infix.testBalloon.framework.testSuite
import io.kotest.assertions.MultiAssertionError
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotEndWith

val IntegrationTest by testSuite {
    test("assertSoftly", testConfig = TestConfig.kotestAssertionsSupport()) {
        @OptIn(TestBalloonExperimentalApi::class)
        if (testPlatform.type == TestPlatform.Type.NATIVE) return@test // TODO: Add Native support in Kotest
        shouldThrow<MultiAssertionError> {
            assertSoftly {
                "Expect failure 1!" shouldNotEndWith "!"
                "Expect failure 2!" shouldNotEndWith "!"
            }
        }.message shouldContain "The following 2 assertions failed:"
    }
}
