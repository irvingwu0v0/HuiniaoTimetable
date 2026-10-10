package com.irvingwu.huiniaotable

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.irvingwu.huiniaotable.notify.ClassNotifier

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.irvingwu.huiniaotable", appContext.packageName)
    }

    @Test
    fun testLiveUpdateNotificationDoesNotCrash() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val result = ClassNotifier.previewLiveUpdate(appContext)
        assertNotNull(result)
        ClassNotifier.clearNotifications(appContext)
    }

    @Test
    fun testReminderNotificationDoesNotCrash() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val result = ClassNotifier.previewReminder(appContext)
        assertNotNull(result)
        ClassNotifier.clearNotifications(appContext)
    }
}
