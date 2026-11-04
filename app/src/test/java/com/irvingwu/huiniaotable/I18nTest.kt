package com.irvingwu.huiniaotable

import com.irvingwu.huiniaotable.data.WeekendMode
import com.irvingwu.huiniaotable.data.formatWeeks
import com.irvingwu.huiniaotable.data.weekdayName
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

class I18nTest {

    private val defaultLocale = Locale.getDefault()

    @Before
    fun useEnglishLocale() {
        Locale.setDefault(Locale.ENGLISH)
    }

    @After
    fun restoreLocale() {
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun `English labels are selected for English locale`() {
        assertEquals("Monday", weekdayName(1))
        assertEquals("Weeks 2-18", formatWeeks((2..18).toList()))
        assertEquals("Auto", WeekendMode.Auto.label)
        assertEquals("Show", WeekendMode.Always.label)
    }
}
