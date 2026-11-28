package com.irvingwu.huiniaotable

import com.irvingwu.huiniaotable.data.Course
import com.irvingwu.huiniaotable.data.KbParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Locale

/**
 * 用与教务系统真实页面同构的样本验证课表解析逻辑。
 * 样本里的课程/教师都是虚构的，不含真实学生数据。
 */
class KbParserTest {

    private val defaultLocale = Locale.getDefault()

    @Before
    fun useChineseLocale() {
        Locale.setDefault(Locale.CHINA)
    }

    @After
    fun restoreLocale() {
        Locale.setDefault(defaultLocale)
    }

    private val html: String = checkNotNull(javaClass.classLoader?.getResourceAsStream("kb_sample.html")) {
        "测试资源 kb_sample.html 缺失"
    }.use { it.readBytes().toString(Charsets.UTF_8) }

    private val parsed by lazy { KbParser.parse(html) }

    @Test
    fun `解析学期与周次下拉框`() {
        assertEquals("2026-2027-1", parsed.schedule.term)
        assertEquals(1, parsed.terms.size)
        assertTrue(parsed.terms.first().selected)
        assertEquals(20, parsed.schedule.maxWeek)
        // 周次下拉框有 20 项，但真正有课的最大周次是 18，周次选择器只到 18。
        assertEquals(18, parsed.schedule.lastTeachingWeek)
    }

    @Test
    fun `解析大节行`() {
        assertEquals(
            (1..4).map { it.toString() to (it to it) },
            parsed.schedule.slotRows.map { it.label to (it.startSlot to it.endSlot) },
        )
    }

    @Test
    fun `解析课程名 教师 教室 周次 节次`() {
        val math = parsed.schedule.courses.first { it.name == "高等数学(理科上册)" }
        assertEquals("张老师", math.teacher)
        assertEquals("第一教学楼501", math.room)
        assertEquals("第一教学楼", math.campus)
        assertEquals("计算机1班", math.classLabel)
        assertEquals(1, math.dayOfWeek)
        assertEquals(1, math.startSlot)
        assertEquals(2, math.endSlot)
        assertEquals((2..18).toList(), math.weeks)
        assertEquals(0, math.rowIndex)
        assertEquals("2-18周", math.weekText)
        assertEquals("星期一 第1-2节", "${math.dayText} ${math.slotText}")
    }

    @Test
    fun `精确节次来自详情块的方括号`() {
        val cs = parsed.schedule.courses.first { it.name == "计算机导论" }
        assertEquals(3, cs.startSlot)
        assertEquals(4, cs.endSlot)
        assertEquals(2, cs.rowIndex)
        assertEquals((2..18 step 2).toList(), cs.weeks)
        assertEquals("钱老师", cs.teacher)
    }

    @Test
    fun `单周课程按奇数周解析`() {
        val ethics = parsed.schedule.courses.first { it.name == "思想道德与法治" }
        assertEquals(listOf(1, 3, 5, 7, 9, 11, 13, 15), ethics.weeks)
        assertEquals("王老师", ethics.teacher)
    }

    @Test
    fun `同一格里的两门课各自匹配到自己的教师`() {
        val pe = parsed.schedule.courses.first { it.name == "大学体育(1)" }
        val policy = parsed.schedule.courses.first { it.name == "形势与政策" }
        assertEquals("赵老师", pe.teacher)
        assertEquals((2..9).toList(), pe.weeks)
        assertEquals("周老师", policy.teacher)
        assertEquals((10..18).toList(), policy.weeks)
        assertEquals(6, pe.dayOfWeek)
        assertEquals(6, policy.dayOfWeek)
    }

    @Test
    fun `按周次筛选格子内容`() {
        assertEquals(1, parsed.schedule.coursesAt(day = 1, rowIndex = 0, week = 5).size)
        assertTrue(parsed.schedule.coursesAt(day = 6, rowIndex = 0, week = 5).any { it.name == "大学体育(1)" })
        assertTrue(parsed.schedule.coursesAt(day = 6, rowIndex = 0, week = 12).any { it.name == "形势与政策" })
        assertTrue(parsed.schedule.coursesAt(day = 6, rowIndex = 0, week = 12).none { it.name == "大学体育(1)" })
        assertTrue(parsed.schedule.coursesAt(day = 2, rowIndex = 0, week = 1).isEmpty())
    }

    @Test
    fun `备注行不会被当成课表格子`() {
        assertEquals(4, parsed.schedule.slotRows.size)
        assertTrue(parsed.schedule.courses.none { it.name.contains("教材") })
    }

    @Test
    fun `解析无课表课程`() {
        assertEquals(1, parsed.schedule.unscheduled.size)
        val course = parsed.schedule.unscheduled.first()
        assertEquals("专业导论", course.name)
        assertEquals("吴老师", course.teacher)
        assertEquals("软件工程1班", course.classLabel)
        assertEquals("CS101", course.code)
        assertEquals("必修", course.category)
        assertEquals("理论课", course.attribute)
    }

    @Test
    fun `周次文本的各种写法`() {
        assertEquals((2..18).toList(), KbParser.parseWeeks("2-18(周)"))
        assertEquals((2..18).toList(), KbParser.parseWeeks("2-18(周)[01-02节]"))
        assertEquals(listOf(2, 4, 6), KbParser.parseWeeks("2,4,6(周)"))
        assertEquals(listOf(3), KbParser.parseWeeks("3周"))
        assertEquals(listOf(1, 3, 5), KbParser.parseWeeks("1-5周(单)"))
        assertEquals(listOf(2, 4, 6), KbParser.parseWeeks("1-6(周)(双)"))
        assertEquals(emptyList<Int>(), KbParser.parseWeeks(""))
        assertEquals(emptyList<Int>(), KbParser.parseWeeks("周次待定"))
    }

    @Test
    fun `周次未知的课程按每周都有处理`() {
        val course = Course(
            name = "周次待定课程",
            dayOfWeek = 1,
            startSlot = 1,
            endSlot = 2,
            weeks = emptyList(),
        )
        assertTrue(course.activeIn(1))
        assertTrue(course.activeIn(18))
    }

    @Test
    fun `页面结构无法识别时抛出可读异常`() {
        assertNull(
            runCatching { KbParser.parse("<html><body>请先登录</body></html>") }.getOrNull(),
        )
        assertNotNull(
            runCatching { KbParser.parse("<html><body>请先登录</body></html>") }.exceptionOrNull(),
        )
    }

    private val zhengfangHtml = """
        <html><body>
        <table id="kbtable">
          <tr><th>节次</th><th>星期一</th><th>星期二</th></tr>
          <tr><th>第1-2节</th>
            <td>高等数学<br>1-16周<br>张三<br>一教101</td>
            <td>大学英语<br>1-8周<br>李四<br>二教202</td>
          </tr>
          <tr><th>第3-4节</th>
            <td>高等数学<br>1-16周<br>张三<br>一教101</td>
            <td>数据结构<br>3-15周(单)<br>王五<br>三教303</td>
          </tr>
        </table>
        </body></html>
    """.trimIndent()

    private val kingosoftHtml = """
        <html><body>
        <table id="Table1">
          <tr><td>节次</td><td>周一</td><td>周二</td></tr>
          <tr><td>第5-6节</td>
            <td><span>线性代数</span><br><span>2-12周</span><br><span>赵六</span><br><span>四教404</span></td>
            <td></td>
          </tr>
          <tr><td>第7-8节</td>
            <td><span>线性代数</span><br><span>2-12周</span><br><span>赵六</span><br><span>四教404</span></td>
            <td></td>
          </tr>
        </table>
        </body></html>
    """.trimIndent()

    @Test
    fun `识别正方课表并合并跨大节的同一门课`() {
        val result = KbParser.parse(zhengfangHtml)
        assertEquals(com.irvingwu.huiniaotable.data.SchoolFamily.Zhengfang, KbParser.detectFamily(zhengfangHtml))
        val math = result.schedule.courses.first { it.name == "高等数学" }
        assertEquals(1, math.startSlot)
        assertEquals(4, math.endSlot)
        assertEquals("张三", math.teacher)
        assertEquals("一教101", math.room)
        assertEquals((1..16).toList(), math.weeks)
        assertEquals(1, result.schedule.courses.count { it.dayOfWeek == 1 })
    }

    @Test
    fun `识别青果课表并按单双周拆分课程`() {
        val result = KbParser.parse(kingosoftHtml)
        assertEquals(com.irvingwu.huiniaotable.data.SchoolFamily.Kingosoft, KbParser.detectFamily(kingosoftHtml))
        val linear = result.schedule.courses.first { it.name == "线性代数" }
        assertEquals(5, linear.startSlot)
        assertEquals(8, linear.endSlot)
        assertEquals((2..12).toList(), linear.weeks)
        assertEquals("四教404", linear.room)
    }

    @Test
    fun `跨大节的三节课合并为一个连续方块`() {
        val html = """
            <table id="kbtable">
              <tr><th>节次</th><th>星期一</th></tr>
              <tr><th>第1-2节</th><td>实验课<br>1-16周<br>孙七<br>实验室1</td></tr>
              <tr><th>第3-4节</th><td>实验课<br>1-16周<br>孙七<br>实验室1<br>[03节]</td></tr>
            </table>
        """.trimIndent()
        val course = KbParser.parse(html).schedule.courses.single { it.name == "实验课" }
        assertEquals(1, course.startSlot)
        assertEquals(3, course.endSlot)
    }
}
