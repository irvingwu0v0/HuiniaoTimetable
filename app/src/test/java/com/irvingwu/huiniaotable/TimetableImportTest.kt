package com.irvingwu.huiniaotable

import com.irvingwu.huiniaotable.data.*
import com.irvingwu.huiniaotable.ui.dayLayout
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class TimetableImportTest {
    @Test
    fun mergedHeadersAndCoursesKeepLogicalColumns() {
        val html = """<table><tr><th colspan=2>节次</th><th>星期一</th><th>星期二</th></tr>
            <tr><td rowspan=3>上午</td><td>1</td><td rowspan=3>力学<br>1-8周(单)</td><td></td></tr>
            <tr><td>2</td><td>英语<br>教师：李老师<br>地点：A302</td></tr>
            <tr><td>3</td><td></td></tr></table>"""
        val schedule = KbParser.parse(html).schedule
        assertEquals(3, schedule.slotRows.size)
        val physics = schedule.courses.single { it.name == "力学" }
        assertEquals(1..3, physics.startSlot..physics.endSlot)
        assertEquals(listOf(1, 3, 5, 7), physics.weeks)
        val english = schedule.courses.single { it.name == "英语" }
        assertEquals(2 to 2, english.dayOfWeek to english.startSlot)
        assertEquals("李老师", english.teacher)
        assertEquals("A302", english.room)
    }

    @Test
    fun structuredZhengfangSeparatesPeriodNumbersFromTeachingWeeks() {
        val block = """<div class=timetable_con><span class=title>线性代数</span>
            <p><i title="节/周"></i><span class=timetable-content>(1-2节)5-8周(双)</span></p>
            <p><i title="教师"></i><span class=timetable-content>王老师</span></p></div>"""
        val course = KbParser.parse("<table id=kbtable><tr><th></th><th>周三</th></tr><tr><td>1</td><td>$block</td></tr></table>").schedule.courses.single()
        assertEquals(1..2, course.startSlot..course.endSlot)
        assertEquals(listOf(6, 8), course.weeks)
        assertEquals("王老师", course.teacher)
    }

    @Test
    fun columnNamesNotColumnOrderIdentifyCourseList() {
        val schedule = KbParser.parse("""<table><tr><td>教室</td><td>起始节次</td><td>结束节次</td><td>课程名</td><td>上课星期</td><td>周次</td></tr>
            <tr><td>202</td><td>3</td><td>5</td><td>物理</td><td>7</td><td>1-6周(单),8-12周(双)</td></tr></table>""").schedule
        val course = schedule.courses.single()
        assertEquals(7 to 2, course.dayOfWeek to course.rowIndex)
        assertEquals(listOf(1, 3, 5, 8, 10, 12), course.weeks)
        assertEquals(5, course.endSlot)
    }

    @Test
    fun xlsxUsesSharedStringsAndHorizontalMergeWithoutJoiningAcrossGaps() {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            fun add(path: String, xml: String) {
                zip.putNextEntry(ZipEntry(path)); zip.write(xml.toByteArray()); zip.closeEntry()
            }
            add("xl/worksheets/sheet1.xml", """<worksheet><sheetData><row>
                <c r="B1"><v>1</v></c><c r="C1"><v>2</v></c><c r="D1"><v>3</v></c><c r="E1"><v>4</v></c></row>
                <row><c r="A2" t="inlineStr"><is><t>星期五</t></is></c><c r="B2" t="s"><v>0</v></c><c r="E2" t="s"><v>0</v></c></row>
                </sheetData><mergeCells><mergeCell ref="B2:C2"/></mergeCells></worksheet>""")
            add("xl/sharedStrings.xml", "<sst><si><t>物理实验\n1-12周\n实验楼201</t></si></sst>")
        }
        val schedule = TimetableFile.parse(bytes.toByteArray().inputStream()).schedule
        assertEquals(listOf(1..2, 4..4), schedule.courses.map { it.startSlot..it.endSlot })
        assertEquals(listOf("物理实验"), schedule.courses.map { it.name }.distinct())
        assertEquals("实验楼201", schedule.courses.first().room)
    }

    @Test
    fun cachedSecondHalfMoveMigratesOnceAndKeepsItsWeekAndWeekday() {
        val course = Course("实验", startSlot = 2, endSlot = 2, rowIndex = 0, weeks = listOf(3))
        val old = Schedule(courses = listOf(course), slotRows = SlotRow.legacyRows)
        val key = "3#${course.key}"
        val moved = old.placementsByPeriod(mapOf(key to 40501))
        assertEquals(40503, moved[key])
        val schedule = old.byPeriod()
        assertSame(schedule, schedule.byPeriod())
        val state = AppState(schedule = schedule, coursePlacements = moved)
        assertEquals(listOf(3 to 4), dayLayout(state, 5, 4, schedule.slotRows).map { it.row to it.end })
        assertEquals(emptyList<Any>(), dayLayout(state, 1, 3, schedule.slotRows))
    }

    @Test
    fun simultaneousClassesUseSeparateLanesButAdjacentClassesDoNot() {
        val schedule = Schedule(courses = listOf(Course("化学", endSlot = 3), Course("实验", startSlot = 2, endSlot = 2),
            Course("英语", startSlot = 4, endSlot = 4))).byPeriod()
        val entries = dayLayout(AppState(schedule = schedule), 1, 1, schedule.slotRows)
        assertEquals(listOf(0 to 3, 1 to 2, 3 to 4), entries.map { it.row to it.end })
        assertEquals(listOf(0 to 2, 1 to 2, 0 to 1), entries.map { it.lane to it.lanes })
    }
}
