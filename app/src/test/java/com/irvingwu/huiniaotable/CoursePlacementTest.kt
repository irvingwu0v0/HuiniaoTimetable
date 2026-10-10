package com.irvingwu.huiniaotable

import com.irvingwu.huiniaotable.data.AppState
import com.irvingwu.huiniaotable.data.Course
import com.irvingwu.huiniaotable.data.Schedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 调课（拖动 / 跳到指定周放置）的排布语义：
 * 1. 同周拖动：将本周的原格子挪到新空格，其它周保持原样；
 * 2. 跨周调课：将源周的那一次课换到目标周上，源周的原位消失；目标周原本的课不受影响正常保留（该周出现两节课）；
 * 3. 恢复默认：清除后各周均回归教务初始排布。
 */
class CoursePlacementTest {

    private fun course() = Course(
        name = "大学语文",
        teacher = "李老师",
        room = "第一教学楼101",
        dayOfWeek = 2,
        startSlot = 3,
        endSlot = 4,
        weeks = (1..16).toList(),
        rowIndex = 1,
    )

    private fun stateOf(vararg placements: Pair<String, Int>) = AppState(
        schedule = Schedule(courses = listOf(course()), lastTeachingWeek = 18),
        coursePlacements = placements.toMap(),
    )

    private fun packMove(targetWeek: Int, day: Int, row: Int) =
        targetWeek * 10000 + day * 100 + row

    @Test
    fun `默认位置即教务排课位置`() {
        val c = course()
        val s = stateOf()
        assertEquals(2 to 1, s.positionOf(c, 3))
        assertTrue(s.displayCoursesAt(2, 1, 3).contains(c))
        assertFalse(s.isCourseMoved(c))
    }

    @Test
    fun `同周调课只挪走本格 不改动其它周`() {
        val c = course()
        val s = stateOf("3#${c.key}" to packMove(3, 4, 2))
        assertEquals(4 to 2, s.positionOf(c, 3))
        assertFalse(s.displayCoursesAt(2, 1, 3).contains(c))
        assertTrue(s.displayCoursesAt(4, 2, 3).contains(c))
        // 其它周不受影响
        assertEquals(2 to 1, s.positionOf(c, 4))
        assertTrue(s.displayCoursesAt(2, 1, 4).contains(c))
        assertEquals(emptyList<Int>(), s.removedWeeksOf(c))
        assertEquals(listOf(3), s.placementWeeksOf(c))
    }

    @Test
    fun `跨周调课后源周消失 目标周原有课正常保留 且显示新调入的课`() {
        val c = course()
        // 第 3 周的课调到第 5 周周四第 3 大节（day=4, row=2）
        val s = stateOf("3#${c.key}" to packMove(5, 4, 2))

        // 源周（第 3 周）：原位消失，变成空格
        assertFalse("源周原位不应再显示", s.displayCoursesAt(2, 1, 3).contains(c))
        assertTrue("源周原格应变为空格", s.isSlotEmpty(2, 1, 3))

        // 目标周（第 5 周）：本周原有的课（周二第 2 大节）必须正常保留！那一周的课不能消失！
        assertTrue("目标周原本的课必须正常保留", s.displayCoursesAt(2, 1, 5).contains(c))
        // 目标周（第 5 周）：调过来的新课在周四第 3 大节正常显示
        assertTrue("目标周新调入的课应正常显示", s.displayCoursesAt(4, 2, 5).contains(c))

        // 未被调动的其它周（第 4 周）保持原样
        assertTrue("未调动的周次保持原样", s.displayCoursesAt(2, 1, 4).contains(c))

        assertEquals("源周应被记录在调离周列表中", listOf(3), s.removedWeeksOf(c))
        assertEquals("目标周应被记录在调入周列表中", listOf(5), s.placementWeeksOf(c))
        assertTrue(s.isCourseMoved(c))
        assertEquals(1, s.movedCourseCount)
    }

    @Test
    fun `目标周内同时存在原排课与调入课时 各自位置独立`() {
        val c = course()
        // 第 1 周调至第 2 周周五第 1 大节；第 2 周原本的课调至第 2 周周一第 1 大节
        val s = stateOf(
            "1#${c.key}" to packMove(2, 5, 0),
            "2#${c.key}" to packMove(2, 1, 0),
        )

        val positions = s.positionsOf(c, 2)
        assertEquals(2, positions.size)
        assertTrue(positions.contains(5 to 0))
        assertTrue(positions.contains(1 to 0))

        assertTrue(s.displayCoursesAt(5, 0, 2).contains(c))
        assertTrue(s.displayCoursesAt(1, 0, 2).contains(c))
        assertFalse(s.displayCoursesAt(2, 1, 2).contains(c)) // 第 2 周原位置已移走
        assertFalse(s.displayCoursesAt(2, 1, 1).contains(c)) // 第 1 周已调离
    }

    @Test
    fun `兼容旧版同周两位数打包格式`() {
        val c = course()
        // 旧版格式：3#key -> 402（周四第 2 大节）
        val s = stateOf("3#${c.key}" to 402)
        assertTrue(s.displayCoursesAt(4, 2, 3).contains(c))
        assertFalse(s.displayCoursesAt(2, 1, 3).contains(c))
        assertTrue(s.displayCoursesAt(2, 1, 4).contains(c))
    }
}
