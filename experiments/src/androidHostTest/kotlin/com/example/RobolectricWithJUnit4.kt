package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RobolectricWithJUnit4 {
    @Test
    fun hello() {
        val app = getApplicationContext<Application>()
        println("hello: app=$app")
    }
}
