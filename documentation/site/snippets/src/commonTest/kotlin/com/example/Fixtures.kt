@file:Suppress("unused", "RedundantSuspendModifier")

package com.example

import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertEquals

// --8<-- [start:starred-users]
val StarredUsers by testSuite {
    val starRepository = testFixture {
        // (1)!
        StarRepository() // (2)!
    } closeWith {
        disconnect() // (3)!
    }

    test("alina") {
        assertEquals(4, starRepository().userStars("alina")) // (4)!
    }

    test("peter") {
        assertEquals(3, starRepository().userStars("peter")) // (5)!
    }
} // (6)!
// --8<-- [end:starred-users]

private class StarRepository {
    suspend fun userStars(user: String): Int = 0
    suspend fun disconnect() {}
}
