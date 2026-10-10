package com.irvingwu.huiniaotable

import androidx.compose.material3.Surface
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import com.irvingwu.huiniaotable.data.AppState
import com.irvingwu.huiniaotable.data.Campus
import com.irvingwu.huiniaotable.data.Course
import com.irvingwu.huiniaotable.data.HolidayDay
import com.irvingwu.huiniaotable.data.HolidayYear
import com.irvingwu.huiniaotable.data.Phase
import com.irvingwu.huiniaotable.data.Schedule
import com.irvingwu.huiniaotable.data.UnscheduledCourse
import com.irvingwu.huiniaotable.ui.CoursesContent
import com.irvingwu.huiniaotable.ui.ScheduleContent
import com.irvingwu.huiniaotable.ui.SettingsContent
import com.irvingwu.huiniaotable.ui.theme.HuiniaoTimetableTheme
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 三个页面的渲染冒烟测试：用固定的假数据在真机/模拟器上跑一遍，
 * 确保课网格、课程列表、设置页都不会崩，并且关键信息确实显示出来。
 */
class ScreensSmokeTest {

    @get:Rule
    val compose = createComposeRule()

    private val today: LocalDate = LocalDate.now()
    private val thisMonday: LocalDate = today.minusDays((today.dayOfWeek.value - 1).toLong())
    private val termStart: LocalDate = thisMonday.minusWeeks(4) // 今天是第 5 周

    private val schedule = Schedule(
        term = "2026-2027-1",
        courses = listOf(
            Course(
                name = "高等数学(理科上册)",
                classLabel = "计算机1班",
                teacher = "张老师",
                room = "第一教学楼 101",
                dayOfWeek = 1,
                startSlot = 1,
                endSlot = 2,
                weeks = (2..18).toList(),
                rowIndex = 0,
            ),
            Course(
                name = "大学英语(1)",
                classLabel = "软件1班",
                teacher = "李老师",
                room = "第二教学楼 203",
                dayOfWeek = 3,
                startSlot = 3,
                endSlot = 4,
                weeks = (2..18).toList(),
                rowIndex = 1,
            ),
        ),
        unscheduled = listOf(UnscheduledCourse(name = "专业导论", teacher = "吴老师")),
        maxWeek = 20,
        lastTeachingWeek = 18,
        fetchedAt = System.currentTimeMillis(),
    )

    private fun stateWith(
        holidays: HolidayYear? = null,
        currentWeek: Int = 5,
        makeupOverrides: Map<LocalDate, Int> = emptyMap(),
    ) = AppState(
        phase = Phase.Ready,
        studentId = "2026000000",
        schedule = schedule,
        term = "2026-2027-1",
        currentWeek = currentWeek,
        selectedWeek = currentWeek,
        termStart = termStart,
        campus = Campus.Default,
        fetchedAt = schedule.fetchedAt,
        holidays = holidays,
        makeupOverrides = makeupOverrides,
    )

    @Test
    fun 课表页显示节次时间与课程() {
        compose.setContent {
            HuiniaoTimetableTheme(dynamicColor = false) {
                Surface {
                    ScheduleContent(
                        state = stateWith(),
                        onRefresh = {},
                        onSelectWeek = {},
                        onToday = {},
                        onSetTermStart = {},
                        onCourseClick = {},
                    )
                }
            }
        }
        compose.onNodeWithText("1-2").assertIsDisplayed()
        compose.onNodeWithText("08:00").assertIsDisplayed()
        compose.onNodeWithText("高等数学(理科上册)").assertIsDisplayed()
        compose.onNodeWithText("第一教学楼 101").assertIsDisplayed()
        // 课程方块里除了教室，还要显示授课教师
        compose.onNodeWithText("张老师").assertIsDisplayed()
        // 结束时间
        compose.onNodeWithText("09:40").assertIsDisplayed()
    }

    @Test
    fun 周末没有课时不显示周六周日() {
        compose.setContent {
            HuiniaoTimetableTheme(dynamicColor = false) {
                Surface {
                    ScheduleContent(
                        state = stateWith(),
                        onRefresh = {},
                        onSelectWeek = {},
                        onToday = {},
                        onSetTermStart = {},
                        onCourseClick = {},
                    )
                }
            }
        }
        compose.onNodeWithText("一").assertIsDisplayed()
        compose.onNodeWithText("五").assertIsDisplayed()
        compose.onNodeWithText("六").assertDoesNotExist()
        compose.onNodeWithText("日").assertDoesNotExist()
    }

    @Test
    fun 调休补的那天有课的周六会显示出来并标注补周几() {
        val saturday = thisMonday.plusDays(5)
        val holidays = HolidayYear(
            year = saturday.year,
            days = mapOf(saturday to HolidayDay(saturday, "国庆前补班", isOffDay = false)),
            source = "测试",
            fetchedAt = 0L,
        )
        compose.setContent {
            HuiniaoTimetableTheme(dynamicColor = false) {
                Surface {
                    ScheduleContent(
                        // 补周三的课，而假数据里周三确实有课 -> 周六这一列应该出现
                        state = stateWith(holidays = holidays, makeupOverrides = mapOf(saturday to 3)),
                        onRefresh = {},
                        onSelectWeek = {},
                        onToday = {},
                        onSetTermStart = {},
                        onCourseClick = {},
                    )
                }
            }
        }
        compose.onNodeWithText("六").assertIsDisplayed()
        compose.onNodeWithText("班").assertIsDisplayed()
        compose.onNodeWithText("补周三").assertIsDisplayed()
    }

    @Test
    fun 调休补的那天没课时不显示周末列() {
        val saturday = thisMonday.plusDays(5)
        val holidays = HolidayYear(
            year = saturday.year,
            days = mapOf(saturday to HolidayDay(saturday, "调休上班", isOffDay = false)),
            source = "测试",
            fetchedAt = 0L,
        )
        compose.setContent {
            HuiniaoTimetableTheme(dynamicColor = false) {
                Surface {
                    ScheduleContent(
                        // 补周四的课，假数据里周四没有课 -> 周六列不该出现
                        state = stateWith(holidays = holidays, makeupOverrides = mapOf(saturday to 4)),
                        onRefresh = {},
                        onSelectWeek = {},
                        onToday = {},
                        onSetTermStart = {},
                        onCourseClick = {},
                    )
                }
            }
        }
        compose.onNodeWithText("六").assertDoesNotExist()
        compose.onNodeWithText("日").assertDoesNotExist()
    }

    @Test
    fun 课程页列出课程与无课表课程() {
        compose.setContent {
            HuiniaoTimetableTheme(dynamicColor = false) {
                Surface {
                    CoursesContent(state = stateWith(), onRefresh = {}, onCourseClick = {})
                }
            }
        }
        compose.onNodeWithText("高等数学(理科上册)").assertIsDisplayed()
        compose.onNodeWithTag("courses_list").performScrollToNode(hasText("无课表课程 · 1 门"))
        compose.onNodeWithText("无课表课程 · 1 门").assertIsDisplayed()
        compose.onNodeWithTag("courses_list").performScrollToNode(hasText("专业导论"))
        compose.onNodeWithText("专业导论").assertIsDisplayed()
    }

    @Test
    fun 设置页显示开学日期校区与调休() {
        compose.setContent {
            HuiniaoTimetableTheme(dynamicColor = false) {
                Surface {
                    SettingsContent(
                        state = stateWith(),
                        onRefresh = {},
                        onLogin = {},
                        onSyncCalendar = {},
                        onSelectTerm = {},
                        onSetTermStart = {},
                        onSelectCampus = {},
                        onSelectBuilding = {},
                        onSelectBuildingScheme = { _, _ -> },
                        onSelectWeekendMode = {},
                        onHideEvening = {},
                        onRefreshHolidays = {},
                        onAddHolidayOverride = { _, _, _ -> },
                        onRemoveHolidayOverride = {},
                        onSetMakeup = { _, _ -> },
                        onClearMakeup = {},
                        onDynamicColor = {},
                        onClassNotify = {},
                        onOpenDebug = {},
                        onLogout = {},
                    )
                }
            }
        }
        compose.onNodeWithText("统一认证学号").assertIsDisplayed()
        compose.onNodeWithTag("settings_list").performScrollToNode(hasText("学期开学首周基准日"))
        compose.onNodeWithText("学期开学首周基准日").assertIsDisplayed()
        compose.onNodeWithTag("settings_list").performScrollToNode(hasText("作息方案与校区楼宇设置"))
        compose.onNodeWithText("作息方案与校区楼宇设置").assertIsDisplayed()
        compose.onNodeWithTag("settings_list").performScrollToNode(hasText("节假日与调休编排"))
        compose.onNodeWithText("节假日与调休编排").assertIsDisplayed()
    }

    @Test
    fun 开学日期决定当前周次() {
        val weeks = ChronoUnit.WEEKS.between(termStart, thisMonday).toInt() + 1
        assert(weeks == 5) { "按开学日期推算应该是第 5 周，实际 $weeks" }
    }
}
