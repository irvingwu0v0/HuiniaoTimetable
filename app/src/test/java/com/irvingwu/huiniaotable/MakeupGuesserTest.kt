package com.irvingwu.huiniaotable

import com.irvingwu.huiniaotable.data.AppState
import com.irvingwu.huiniaotable.data.HolidayDay
import com.irvingwu.huiniaotable.data.HolidayYear
import com.irvingwu.huiniaotable.data.MakeupGuesser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * 调休补课推断：全国数据里只有「哪天放假、哪天上班」，没有「补周几的课」，
 * 这里验证按「补假期里被吞掉的最后一个工作日」推断的结果（用 2026 年真实安排）。
 */
class MakeupGuesserTest {

    /** 2026 年真实放假块 + 调休上班日。 */
    private fun holidayMap(): Map<LocalDate, HolidayDay> {
        val blocks = listOf(
            Triple("2026-01-01", "2026-01-03", "元旦"),
            Triple("2026-02-15", "2026-02-23", "春节"),
            Triple("2026-04-04", "2026-04-06", "清明节"),
            Triple("2026-05-01", "2026-05-05", "劳动节"),
            Triple("2026-06-19", "2026-06-21", "端午节"),
            Triple("2026-09-25", "2026-09-27", "中秋节"),
            Triple("2026-10-01", "2026-10-07", "国庆节"),
        )
        val makeups = listOf(
            "2026-01-04", "2026-02-14", "2026-02-28",
            "2026-05-09", "2026-09-20", "2026-10-10",
        )
        val out = linkedMapOf<LocalDate, HolidayDay>()
        for ((start, end, name) in blocks) {
            var date = LocalDate.parse(start)
            val last = LocalDate.parse(end)
            while (!date.isAfter(last)) {
                out[date] = HolidayDay(date, name, isOffDay = true)
                date = date.plusDays(1)
            }
        }
        for (raw in makeups) {
            val date = LocalDate.parse(raw)
            out[date] = HolidayDay(date, "调休上班", isOffDay = false)
        }
        return out
    }

    private fun guessOf(date: String): Int? =
        MakeupGuesser.guess(holidayMap())[LocalDate.parse(date)]

    @Test
    fun `国庆后补班推断为补周三`() {
        // 国庆 10-01~10-07 吞掉的工作日：10-01(四) 10-02(五) 10-05(一) 10-06(二) 10-07(三)，最后一个是周三
        assertEquals(3, guessOf("2026-10-10"))
    }

    @Test
    fun `中秋前补班推断为补周五`() {
        // 中秋 09-25~09-27 只吞掉 09-25(五)
        assertEquals(5, guessOf("2026-09-20"))
    }

    @Test
    fun `元旦后补班推断为补周五`() {
        // 元旦 01-01~01-03 吞掉 01-01(四) 01-02(五)
        assertEquals(5, guessOf("2026-01-04"))
    }

    @Test
    fun `劳动节后补班推断为补周二`() {
        // 劳动节 05-01~05-05 吞掉 05-01(五) 05-04(一) 05-05(二)
        assertEquals(2, guessOf("2026-05-09"))
    }

    @Test
    fun `春节两个补班日分别对应假期首尾的工作日`() {
        // 春节 02-15~02-23 吞掉 02-16(一)…02-23(一)，两个补班日按顺序对应第一个和最后一个
        assertEquals(1, guessOf("2026-02-14"))
        assertEquals(1, guessOf("2026-02-28"))
    }

    @Test
    fun `普通日子不是补班日`() {
        val guesses = MakeupGuesser.guess(holidayMap())
        assertEquals(6, guesses.size)
        assertTrue(guesses.keys.all { date -> !holidayMap().getValue(date).isOffDay })
    }

    // ---- 调休日对课表的影响 ----

    private val holidays = HolidayYear(
        year = 2026,
        days = holidayMap(),
        source = "测试",
        fetchedAt = 0L,
    )

    @Test
    fun `调休上班日按所补的星期显示课程`() {
        val state = AppState(holidays = holidays)
        val makeupDay = LocalDate.parse("2026-10-10") // 周六
        assertEquals(6, makeupDay.dayOfWeek.value)
        assertEquals("调休日应该补周三的课", 3, state.effectiveDayOfWeek(makeupDay))
        assertEquals("补周三", state.makeupLabelFor(makeupDay, 1))
    }

    @Test
    fun `手动改过以后按手动设置来`() {
        val makeupDay = LocalDate.parse("2026-10-10")
        val patched = AppState(holidays = holidays, makeupOverrides = mapOf(makeupDay to 5))
        assertEquals(5, patched.effectiveDayOfWeek(makeupDay))
        assertEquals("补周五", patched.makeupLabelFor(makeupDay, 1))
    }

    @Test
    fun `设成不补课时按当天本来的星期`() {
        val makeupDay = LocalDate.parse("2026-10-10")
        val patched = AppState(holidays = holidays, makeupOverrides = mapOf(makeupDay to 0))
        assertEquals(6, patched.effectiveDayOfWeek(makeupDay))
        assertNull(patched.makeupLabelFor(makeupDay, 1))
    }

    @Test
    fun `普通上课日不显示补课标签`() {
        val state = AppState(holidays = holidays)
        assertNull(state.makeupLabelFor(LocalDate.parse("2026-09-30"), 1))
        assertEquals(3, state.effectiveDayOfWeek(LocalDate.parse("2026-09-30")))
    }

    @Test
    fun `假期当天不显示补课标签`() {
        val state = AppState(holidays = holidays)
        assertNull(state.makeupLabelFor(LocalDate.parse("2026-10-01"), 1))
    }

    @Test
    fun `节前调休补开始工作日_节后调休补末尾工作日`() {
        // 模拟放假：周四(10-01)至下周三(10-07)
        // 节前调休：周日(09-27)
        // 节后调休：周六(10-10)
        val days = linkedMapOf<LocalDate, HolidayDay>()
        var d = LocalDate.parse("2026-10-01")
        val end = LocalDate.parse("2026-10-07")
        while (!d.isAfter(end)) {
            days[d] = HolidayDay(d, "国庆节", isOffDay = true)
            d = d.plusDays(1)
        }
        val before = LocalDate.parse("2026-09-27") // 周日
        val after = LocalDate.parse("2026-10-10") // 周六
        days[before] = HolidayDay(before, "国庆节", isOffDay = false)
        days[after] = HolidayDay(after, "国庆节", isOffDay = false)

        val guess = MakeupGuesser.guess(days)
        // 节前补假期的第一个工作日：周四 (4)
        assertEquals(4, guess[before])
        // 节后补假期的最后一个工作日：周三 (3)
        assertEquals(3, guess[after])
    }

    @Test
    fun `节后双调休日依序对应倒数工作日`() {
        // 假设周一到周五放假 (05-04 ~ 05-08)，节后周六(05-09)、周日(05-10)调休
        val days = linkedMapOf<LocalDate, HolidayDay>()
        for (i in 4..8) {
            val date = LocalDate.of(2026, 5, i)
            days[date] = HolidayDay(date, "特长假", isOffDay = true)
        }
        val sat = LocalDate.of(2026, 5, 9)
        val sun = LocalDate.of(2026, 5, 10)
        days[sat] = HolidayDay(sat, "特长假", isOffDay = false)
        days[sun] = HolidayDay(sun, "特长假", isOffDay = false)

        val guess = MakeupGuesser.guess(days)
        // 节后两个调休按时间倒数匹配：
        // 第一个调休日(周六)补倒数第二个工作日(周四=4)
        assertEquals(4, guess[sat])
        // 第二个调休日(周日)补倒数第一个工作日(周五=5)
        assertEquals(5, guess[sun])
    }

    @Test
    fun `按节日名称精确关联调休日`() {
        // 两个假期相隔很近（如中秋和国庆），调休日按名称匹配到对应节日
        val days = linkedMapOf<LocalDate, HolidayDay>()
        // 节日A：09-25~09-27
        for (i in 25..27) {
            val date = LocalDate.of(2026, 9, i)
            days[date] = HolidayDay(date, "节日A", isOffDay = true)
        }
        val makeupA = LocalDate.of(2026, 9, 20)
        days[makeupA] = HolidayDay(makeupA, "节日A", isOffDay = false)

        // 节日B：10-01~10-03
        for (i in 1..3) {
            val date = LocalDate.of(2026, 10, i)
            days[date] = HolidayDay(date, "节日B", isOffDay = true)
        }
        val makeupB = LocalDate.of(2026, 10, 4)
        days[makeupB] = HolidayDay(makeupB, "节日B", isOffDay = false)

        val guess = MakeupGuesser.guess(days)
        assertEquals(5, guess[makeupA]) // 节日A 09-25 是周五
        assertEquals(5, guess[makeupB]) // 节日B 10-02 是周五 (10-01周四, 10-02周五, 10-03周六)
    }
}
