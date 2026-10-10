package com.irvingwu.huiniaotable

import com.irvingwu.huiniaotable.data.AppState
import com.irvingwu.huiniaotable.data.CampusBuilding
import com.irvingwu.huiniaotable.data.ClassTimes
import com.irvingwu.huiniaotable.data.ClockTime
import com.irvingwu.huiniaotable.data.Course
import com.irvingwu.huiniaotable.data.HolidayDay
import com.irvingwu.huiniaotable.data.HolidayYear
import com.irvingwu.huiniaotable.data.Schedule
import com.irvingwu.huiniaotable.data.WeekendMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * 灰鸟课表作息规范单元测试：
 * 默认内置内地普通高校标准作息（第 1 节 08:00 开始）；
 * 支持图形化节次起止时间修改与自定义 JSON 方案导入导出。
 */
class ClassTimesTest {

    @Before
    fun setUp() {
        ClassTimes.resetToDefault()
    }

    @Test
    fun `内地高校默认作息时间核验`() {
        val periods = ClassTimes.DEFAULT_MAINLAND_PERIODS
        assertEquals(10, periods.size)

        // 上午 1-4 节
        assertEquals("08:00", periods[0].start)
        assertEquals("08:45", periods[0].end)
        assertEquals("08:55", periods[1].start)
        assertEquals("09:40", periods[1].end)
        assertEquals("10:00", periods[2].start)
        assertEquals("10:45", periods[2].end)
        assertEquals("10:55", periods[3].start)
        assertEquals("11:40", periods[3].end)

        // 下午 5-8 节
        assertEquals("14:00", periods[4].start)
        assertEquals("14:45", periods[4].end)
        assertEquals("14:55", periods[5].start)
        assertEquals("15:40", periods[5].end)
        assertEquals("16:00", periods[6].start)
        assertEquals("16:45", periods[6].end)
        assertEquals("16:55", periods[7].start)
        assertEquals("17:40", periods[7].end)

        // 晚间 9-10 节
        assertEquals("19:00", periods[8].start)
        assertEquals("19:45", periods[8].end)
        assertEquals("19:55", periods[9].start)
        assertEquals("20:40", periods[9].end)

        // 连堂时段文本生成
        assertEquals("08:00-09:40", ClassTimes.timeTextFor(1, 2, ClassTimes.defaultBuilding))
        assertEquals("10:00-11:40", ClassTimes.timeTextFor(3, 4, ClassTimes.defaultBuilding))
        assertEquals("14:00-15:40", ClassTimes.timeTextFor(5, 6, ClassTimes.defaultBuilding))
        assertEquals("16:00-17:40", ClassTimes.timeTextFor(7, 8, ClassTimes.defaultBuilding))
        assertEquals("19:00-20:40", ClassTimes.timeTextFor(9, 10, ClassTimes.defaultBuilding))
    }

    @Test
    fun `图形化修改节次起止时间生效`() {
        ClassTimes.updatePeriod(ClassTimes.defaultBuilding.id, 0, "08:10", "08:55")
        val periods = ClassTimes.periods(ClassTimes.defaultBuilding)
        assertEquals("08:10", periods[0].start)
        assertEquals("08:55", periods[0].end)
        assertEquals("08:10-09:40", ClassTimes.timeTextFor(1, 2, ClassTimes.defaultBuilding))
    }

    @Test
    fun `导出与导入作息配置 JSON 回环测试`() {
        val exported = ClassTimes.exportJson()
        assertTrue(exported.contains("标准作息表"))
        assertFalse("导出的作息 JSON 不应包含 buildingHints", exported.contains("buildingHints"))

        ClassTimes.resetToDefault()
        val success = ClassTimes.importJson(exported)
        assertTrue(success)
        assertEquals("08:00-09:40", ClassTimes.timeTextFor(1, 2, ClassTimes.defaultBuilding))
    }

    @Test
    fun `根据课表建筑名称自动匹配作息方案`() {
        val sampleMultiCampusJson = """
        {
          "name": "高校多校区作息表",
          "version": 1,
          "defaultSchemeId": "MainJiao1",
          "schemes": [
            {
              "id": "MainJiao1",
              "campus": "主校区",
              "name": "第一教学楼",
              "periods": [
                {"slot": 1, "start": "08:00", "end": "08:45"},
                {"slot": 2, "start": "08:55", "end": "09:40"},
                {"slot": 3, "start": "10:00", "end": "10:45"},
                {"slot": 4, "start": "10:55", "end": "11:40"},
                {"slot": 5, "start": "14:00", "end": "14:45"},
                {"slot": 6, "start": "14:55", "end": "15:40"},
                {"slot": 7, "start": "16:00", "end": "16:45"},
                {"slot": 8, "start": "16:55", "end": "17:40"},
                {"slot": 9, "start": "19:00", "end": "19:45"},
                {"slot": 10, "start": "19:55", "end": "20:40"}
              ]
            },
            {
              "id": "MainOther",
              "campus": "主校区",
              "name": "其他楼宇",
              "periods": [
                {"slot": 1, "start": "08:00", "end": "08:45"},
                {"slot": 2, "start": "08:55", "end": "09:40"},
                {"slot": 3, "start": "10:10", "end": "10:55"},
                {"slot": 4, "start": "11:05", "end": "11:50"},
                {"slot": 5, "start": "14:00", "end": "14:45"},
                {"slot": 6, "start": "14:55", "end": "15:40"},
                {"slot": 7, "start": "16:00", "end": "16:45"},
                {"slot": 8, "start": "16:55", "end": "17:40"},
                {"slot": 9, "start": "19:00", "end": "19:45"},
                {"slot": 10, "start": "19:55", "end": "20:40"}
              ]
            },
            {
              "id": "EastZonghe",
              "campus": "东校区",
              "name": "综合教学楼",
              "periods": [
                {"slot": 1, "start": "08:30", "end": "09:15"},
                {"slot": 2, "start": "09:25", "end": "10:10"},
                {"slot": 3, "start": "10:30", "end": "11:15"},
                {"slot": 4, "start": "11:25", "end": "12:10"},
                {"slot": 5, "start": "14:30", "end": "15:15"},
                {"slot": 6, "start": "15:25", "end": "16:10"},
                {"slot": 7, "start": "16:30", "end": "17:15"},
                {"slot": 8, "start": "17:25", "end": "18:10"},
                {"slot": 9, "start": "19:30", "end": "20:15"},
                {"slot": 10, "start": "20:25", "end": "21:10"}
              ]
            },
            {
              "id": "SouthLab",
              "campus": "南校区",
              "name": "实验楼",
              "periods": [
                {"slot": 1, "start": "08:00", "end": "08:45"},
                {"slot": 2, "start": "08:55", "end": "09:40"},
                {"slot": 3, "start": "10:00", "end": "10:45"},
                {"slot": 4, "start": "10:55", "end": "11:40"},
                {"slot": 5, "start": "14:00", "end": "14:45"},
                {"slot": 6, "start": "14:55", "end": "15:40"},
                {"slot": 7, "start": "16:00", "end": "16:45"},
                {"slot": 8, "start": "16:55", "end": "17:40"},
                {"slot": 9, "start": "19:00", "end": "19:45"},
                {"slot": 10, "start": "19:55", "end": "20:40"}
              ]
            }
          ]
        }
        """.trimIndent()

        assertTrue(ClassTimes.importJson(sampleMultiCampusJson))
        val defaultB = ClassTimes.defaultBuilding

        // 主校区 第一教学楼101 -> 匹配 MainJiao1
        val jiao1 = ClassTimes.resolveScheme("主校区", "第一教学楼101", emptyMap(), defaultB)
        assertEquals("MainJiao1", jiao1.id)

        // 主校区 图书馆201 -> 匹配 MainOther (其他楼宇)
        val lib = ClassTimes.resolveScheme("主校区", "图书馆201", emptyMap(), defaultB)
        assertEquals("MainOther", lib.id)

        // 东校区 综合教学楼 -> 匹配 EastZonghe
        val east = ClassTimes.resolveScheme("东校区", "综合教学楼302", emptyMap(), defaultB)
        assertEquals("EastZonghe", east.id)

        // 南校区 实验楼 -> 匹配 SouthLab
        val south = ClassTimes.resolveScheme("南校区", "实验楼401", emptyMap(), defaultB)
        assertEquals("SouthLab", south.id)
    }

    @Test
    fun `导入多校区楼宇作息配置正确解析方案`() {
        val sampleJson = """
        {
          "name": "高校多校区作息表",
          "version": 1,
          "defaultSchemeId": "MainJiao1",
          "schemes": [
            {
              "id": "MainJiao1",
              "campus": "主校区",
              "name": "第一教学楼",
              "periods": [
                {"slot": 1, "start": "08:00", "end": "08:45"},
                {"slot": 2, "start": "08:55", "end": "09:40"}
              ]
            },
            {
              "id": "EastLab",
              "campus": "东校区",
              "name": "实验楼",
              "periods": [
                {"slot": 1, "start": "08:30", "end": "09:15"},
                {"slot": 2, "start": "09:25", "end": "10:10"}
              ]
            }
          ]
        }
        """.trimIndent()
        val ok = ClassTimes.importJson(sampleJson)
        assertTrue(ok)
        val b = ClassTimes.findBuilding("MainJiao1")
        assertTrue(b != null)
        if (b != null) {
            assertEquals("08:00-09:40", ClassTimes.timeTextFor(1, 2, b))
        }
    }

    @Test
    fun `用户手动校准楼栋方案优先`() {
        val customScheme = CampusBuilding("custom_1", "第二实验楼方案", "主校区")
        val overrides = mapOf("第一教学楼" to customScheme)
        val resolved = ClassTimes.resolveScheme("第一教学楼", "101", overrides, ClassTimes.defaultBuilding)
        assertEquals(customScheme, resolved)
    }

    // ---- 周末显示策略 ----

    private val monday: LocalDate = LocalDate.of(2026, 8, 31)

    private fun scheduleOn(days: List<Int>): Schedule = Schedule(
        term = "2026-2027-1",
        courses = days.mapIndexed { index, day ->
            Course(
                name = "课程$index",
                dayOfWeek = day,
                startSlot = 1,
                endSlot = 2,
                weeks = (1..18).toList(),
                rowIndex = 0,
            )
        },
        maxWeek = 20,
        lastTeachingWeek = 18,
    )

    @Test
    fun `只有工作日课程时隐藏周末`() {
        val state = AppState(
            schedule = scheduleOn(listOf(1, 2, 3, 4, 5)),
            termStart = monday,
            weekendMode = WeekendMode.Auto,
        )
        assertEquals(listOf(1, 2, 3, 4, 5), state.visibleDays(1))
        assertEquals(listOf(1, 2, 3, 4, 5), state.visibleDays(10))
    }

    @Test
    fun `周六有课就只显示周六这一列`() {
        val state = AppState(
            schedule = scheduleOn(listOf(1, 2, 3, 4, 5, 6)),
            termStart = monday,
            weekendMode = WeekendMode.Auto,
        )
        assertEquals(listOf(1, 2, 3, 4, 5, 6), state.visibleDays(1))
    }

    @Test
    fun `周日有课就只显示周日这一列`() {
        val state = AppState(
            schedule = scheduleOn(listOf(1, 2, 3, 4, 5, 7)),
            termStart = monday,
            weekendMode = WeekendMode.Auto,
        )
        assertEquals(listOf(1, 2, 3, 4, 5, 7), state.visibleDays(1))
    }

    @Test
    fun `周末两天都有课就两列都显示`() {
        val state = AppState(
            schedule = scheduleOn(listOf(1, 3, 6, 7)),
            termStart = monday,
            weekendMode = WeekendMode.Auto,
        )
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), state.visibleDays(1))
    }

    @Test
    fun `调休上班日补的那天有课就显示该列`() {
        val saturday = monday.plusDays(5) // 第 1 周的周六
        val holidays = HolidayYear(
            year = 2026,
            days = mapOf(saturday to HolidayDay(saturday, "国庆前补班", isOffDay = false)),
            source = "测试",
            fetchedAt = 0L,
        )
        val state = AppState(
            schedule = scheduleOn(listOf(1, 2, 3, 4, 5)), // 周三有课
            termStart = monday,
            holidays = holidays,
            makeupOverrides = mapOf(saturday to 3), // 补周三的课
            weekendMode = WeekendMode.Auto,
        )
        assertEquals(listOf(1, 2, 3, 4, 5, 6), state.visibleDays(1))
        assertEquals("补周三", state.makeupLabelFor(saturday, 1))
    }

    @Test
    fun `调休上班日补的那天没课就隐藏该列`() {
        val saturday = monday.plusDays(5)
        val holidays = HolidayYear(
            year = 2026,
            days = mapOf(saturday to HolidayDay(saturday, "国庆前补班", isOffDay = false)),
            source = "测试",
            fetchedAt = 0L,
        )
        val state = AppState(
            schedule = scheduleOn(listOf(1, 2, 3)), // 周四(4)本来就没课
            termStart = monday,
            holidays = holidays,
            makeupOverrides = mapOf(saturday to 4), // 补周四的课 -> 没课
            weekendMode = WeekendMode.Auto,
        )
        assertFalse("补的那天没课，周六列应该隐藏", state.visibleDays(1).contains(6))
        assertEquals(listOf(1, 2, 3, 4, 5), state.visibleDays(1))
    }

    @Test
    fun `强制显示与强制隐藏优先`() {
        val base = AppState(schedule = scheduleOn(listOf(1, 2)), termStart = monday)
        assertEquals((1..7).toList(), base.copy(weekendMode = WeekendMode.Always).visibleDays(1))
        assertEquals(
            listOf(1, 2, 3, 4, 5),
            base.copy(weekendMode = WeekendMode.Hide, schedule = scheduleOn(listOf(1, 6))).visibleDays(1),
        )
    }

    @Test
    fun `最大周次严格以实际排课周次为准避免手势滑到20多周空白`() {
        val sched = scheduleOn(listOf(1, 2)).copy(maxWeek = 30, lastTeachingWeek = 18)
        val state = AppState(schedule = sched, currentWeek = 5, termStart = monday)
        assertEquals("即使教务系统HTML写了30周，最大可用周次也必须是18", 18, state.maxWeek)
    }

    @Test
    fun `默认午休时段自动识别相差最大的两节课`() {
        // 内地高校标准作息中，第 4 节(11:40 结束)与第 5 节(14:00 开始)间隔 140 分钟，为所有节次中最大间隔
        val defaultSlot = ClassTimes.defaultLunchBreakSlot(ClassTimes.defaultBuilding)
        assertEquals(4, defaultSlot)

        // AppState 默认未指定 (0) 时生效第 4 节
        val state = AppState(termStart = monday)
        assertEquals(0, state.lunchBreakSlotOverride)
        assertEquals(4, state.effectiveLunchBreakSlot)
    }

    @Test
    fun `自定义午休开始节次生效与重置`() {
        val state = AppState(termStart = monday, lunchBreakSlotOverride = 2)
        assertEquals(2, state.effectiveLunchBreakSlot)

        val resetState = state.copy(lunchBreakSlotOverride = 0)
        assertEquals(4, resetState.effectiveLunchBreakSlot)
    }

    @Test
    fun `自定义时段列表自动寻找相差最大的相邻两节`() {
        // 模拟特殊高校作息：上午 5 节课，第 5 节 12:30 结束，第 6 节 14:30 开始（间隔 120 分钟）
        val customPeriods = listOf(
            ClockTime("08:00", "08:45"),
            ClockTime("08:55", "09:40"),
            ClockTime("10:00", "10:45"),
            ClockTime("10:55", "11:40"),
            ClockTime("11:45", "12:30"),
            ClockTime("14:30", "15:15"),
            ClockTime("15:25", "16:10"),
        )
        val bestSlot = ClassTimes.findLargestGapSlot(customPeriods)
        assertEquals(5, bestSlot)
    }

    @Test
    fun `动态新增保存与删除作息方案`() {
        val newPeriods = (1..10).map { ClockTime(String.format("%02d:00", it + 7), String.format("%02d:45", it + 7)) }
        ClassTimes.saveScheme("custom_lib", "图书馆方案", "主校区", newPeriods)
        val scheme = ClassTimes.findBuilding("custom_lib")
        assertTrue(scheme != null)
        assertEquals("图书馆方案", scheme?.label)
        assertEquals("主校区", scheme?.campus)

        ClassTimes.deleteScheme("custom_lib")
        assertTrue(ClassTimes.findBuilding("custom_lib") == null)
    }

}
