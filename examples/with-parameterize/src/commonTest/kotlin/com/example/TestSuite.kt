package com.example

import com.benwoodworth.parameterize.parameter
import com.benwoodworth.parameterize.parameterOf
import com.benwoodworth.parameterize.parameterize
import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertFailsWith

val UserTest by testSuite {
    parameterize {
        val userName by parameterOf("", "a", "+", "+foo")
        val role by parameter(User.Role.entries)

        test("The user name '$userName' is invalid with role '$role'") {
            assertFailsWith<IllegalArgumentException> {
                User(userName, role)
            }
        }
    }
}

class User(val name: String, val role: Role) {
    enum class Role {
        FINANCIAL,
        LOGISTICS,
        IT
    }

    init {
        require(name.isEmpty())
        require(name.length >= 3)
        require(name.all { it.isLetter() })
    }
}
