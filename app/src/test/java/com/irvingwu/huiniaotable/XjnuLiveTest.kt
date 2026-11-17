package com.irvingwu.huiniaotable

import com.irvingwu.huiniaotable.data.JwxtClient
import com.irvingwu.huiniaotable.data.LoginResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/** 显式设置临时环境变量才执行；不保存凭据、Cookie 或原始课表。 */
class XjnuLiveTest {
    @Test
    fun `真实客户端登录并读取当前与历史学期`() {
        val studentId = System.getenv("XJNU_STUDENT_ID").orEmpty()
        val password = System.getenv("XJNU_PASSWORD").orEmpty()
        assumeTrue("Live credentials were not supplied", studentId.isNotBlank() && password.isNotBlank())
        val client = JwxtClient(baseUrl = "https://jwxt.xjnu.edu.cn/jsxsd/framework/xsMain.htmlx")
        assertEquals(LoginResult.Success, client.login(studentId, password))
        val current = client.fetchSchedule(null)
        assertTrue(current.terms.isNotEmpty())
        assertTrue(current.schedule.courses.isNotEmpty())
        assertTrue(current.schedule.courses.all { it.dayOfWeek in 1..7 && it.startSlot <= it.endSlot })
        assertTrue(current.schedule.courses.any { it.teacher != null && it.room != null && it.weeks.isNotEmpty() })
        val historical = current.terms.first { it.value != current.schedule.term && it.value < current.schedule.term }
        val previous = client.fetchSchedule(historical.value)
        assertEquals(historical.value, previous.schedule.term)
        assertTrue(previous.schedule.courses.isNotEmpty())
        println("XJNU live verification: current=${current.schedule.term}, entries=${current.schedule.courses.size}, " +
            "rows=${current.schedule.slotRows.size}, terms=${current.terms.size}, " +
            "history=${previous.schedule.term}, historicalEntries=${previous.schedule.courses.size}")
    }
}
