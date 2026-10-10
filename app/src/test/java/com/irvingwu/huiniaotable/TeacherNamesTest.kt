package com.irvingwu.huiniaotable

import com.irvingwu.huiniaotable.data.TeacherNames
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 教务系统教师字段职称清洗测试用例：
 * 去除教务系统在教师姓名后附加的各类高校系列职称。
 */
class TeacherNamesTest {

    @Test
    fun `去掉高等学校教师系列职称`() {
        assertEquals("张伟", TeacherNames.clean("张伟高等学校教师/副教授"))
        assertEquals("李强", TeacherNames.clean("李强高等学校教师/副教授"))
        assertEquals("王雷", TeacherNames.clean("王雷高等学校教师/教授"))
        assertEquals("艾力·买买提", TeacherNames.clean("艾力·买买提高等学校教师/讲师（高校）"))
    }

    @Test
    fun `去掉各种写法的职称`() {
        assertEquals("王敏", TeacherNames.clean("王敏副教授"))
        assertEquals("李四", TeacherNames.clean("李四/讲师"))
        assertEquals("张三", TeacherNames.clean("张三（教授）"))
        assertEquals("赵六", TeacherNames.clean("赵六 高级实验师"))
        assertEquals("钱七", TeacherNames.clean("钱七助教"))
        assertEquals("孙八", TeacherNames.clean("孙八研究员"))
    }

    @Test
    fun `没有职称的姓名原样保留`() {
        assertEquals("张伟", TeacherNames.clean("张伟"))
        assertEquals("迪丽·热巴", TeacherNames.clean("迪丽·热巴"))
        assertEquals("艾力·买买提", TeacherNames.clean("艾力·买买提"))
    }

    @Test
    fun `多位教师逐个处理`() {
        assertEquals("张三、李四", TeacherNames.clean("张三、李四"))
        assertEquals("张三、李四", TeacherNames.clean("张三、李四副教授"))
        assertEquals("张三、李四", TeacherNames.clean("张三教授、李四讲师"))
    }

    @Test
    fun `只有职称时当作没有教师信息`() {
        assertNull(TeacherNames.clean(""))
        assertNull(TeacherNames.clean("   "))
        assertNull(TeacherNames.clean(null))
        assertNull(TeacherNames.clean("副教授"))
        assertNull(TeacherNames.clean("高等学校教师/讲师"))
    }

    @Test
    fun `解析课表时就已经去掉职称`() {
        val html = checkNotNull(javaClass.classLoader?.getResourceAsStream("kb_sample.html"))
            .use { it.readBytes().toString(Charsets.UTF_8) }
        val parsed = com.irvingwu.huiniaotable.data.KbParser.parse(html)
        val math = parsed.schedule.courses.first { it.name == "高等数学(理科上册)" }
        val english = parsed.schedule.courses.first { it.name == "大学英语(1)" }
        assertEquals("张老师", math.teacher)
        assertEquals("李老师", english.teacher)
    }
}
