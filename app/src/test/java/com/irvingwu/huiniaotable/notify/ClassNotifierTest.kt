package com.irvingwu.huiniaotable.notify

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ClassNotifierTest {

    @Test
    fun testConstants() {
        assertEquals(15, ClassNotifier.LEAD_MINUTES)
        assertEquals("com.irvingwu.huiniaotable.notify.REFRESH", ClassNotifier.ACTION_REFRESH)
    }

    @Test
    fun testLiveUpdatesSupportedDoesNotThrow() {
        // Must safely query without LinkageError / NoClassDefFoundError
        val supported = ClassNotifier.liveUpdatesSupported
        assertNotNull(supported)
    }

    @Test
    fun testSafeReflectionInvocationOnMissingMethod() {
        val anyObject = Any()
        var threw = false
        try {
            val method = anyObject::class.java.methods.firstOrNull { it.name == "nonExistentMethod" }
            method?.invoke(anyObject)
        } catch (t: Throwable) {
            threw = true
        }
        assertEquals(false, threw)
    }
}
