package com.irvingwu.huiniaotable.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HolidayClientTest {

    @Test
    fun testHolidayRegionMapping() {
        assertEquals(HolidayRegion.NONE, HolidayRegion.fromCode(null))
        assertEquals(HolidayRegion.NONE, HolidayRegion.fromCode(""))
        assertEquals("国务院公布方案", HolidayRegion.NONE.displayName)
        assertEquals(HolidayRegion.XINJIANG, HolidayRegion.fromCode("xinjiang"))
        assertEquals(HolidayRegion.XIZANG, HolidayRegion.fromCode("xizang"))
        assertEquals(HolidayRegion.NINGXIA, HolidayRegion.fromCode("ningxia"))
        assertEquals(HolidayRegion.GUANGXI, HolidayRegion.fromCode("guangxi"))
        assertEquals(HolidayRegion.NEIMENGGU, HolidayRegion.fromCode("neimenggu"))
    }

    @Test
    fun testRegionalHolidayFetch() {
        val client = HolidayClient()
        val data = client.fetch(2026, HolidayRegion.XINJIANG)
        assertEquals(2026, data.year)
        assertTrue(data.days.isNotEmpty())
        assertTrue(data.source.contains("新疆维吾尔自治区"))

        // 新疆特色节日验证：肉孜节、古尔邦节
        val hasRouzi = data.days.values.any { it.name.contains("肉孜节") }
        val hasGuerbang = data.days.values.any { it.name.contains("古尔邦节") }
        assertTrue("应包含肉孜节", hasRouzi)
        assertTrue("应包含古尔邦节", hasGuerbang)
    }

    @Test
    fun testFetchMergedWithRegion() {
        val client = HolidayClient()
        // 模拟当前时间哪怕处于 2025 年，fetchMerged 也能获取 2026 年自治区数据
        val data = client.fetchMerged(
            today = LocalDate.of(2025, 5, 1),
            region = HolidayRegion.XINJIANG,
            termYear = 2026,
        )
        assertTrue(data.days.isNotEmpty())
        val hasRouzi = data.days.values.any { it.name.contains("肉孜节") }
        val hasGuerbang = data.days.values.any { it.name.contains("古尔邦节") }
        assertTrue("合并数据中应包含肉孜节", hasRouzi)
        assertTrue("合并数据中应包含古尔邦节", hasGuerbang)
    }

    @Test
    fun testNationalHolidayFetch() {
        val client = HolidayClient()
        val data = client.fetch(2026, HolidayRegion.NONE)
        assertEquals(2026, data.year)
        assertTrue(data.days.isNotEmpty())
        assertTrue(data.days.values.any { it.name.contains("春节") })
        assertTrue(data.days.values.any { it.name.contains("国庆节") })
    }

    @Test
    fun testHolidaySourceMapping() {
        assertEquals(HolidaySource.AUTO, HolidaySource.fromCode(null))
        assertEquals(HolidaySource.AUTO, HolidaySource.fromCode(""))
        assertEquals(HolidaySource.GHPROXY, HolidaySource.fromCode("ghproxy"))
        assertEquals(HolidaySource.JSDELIVR, HolidaySource.fromCode("jsdelivr"))
        assertEquals(HolidaySource.FASTLY, HolidaySource.fromCode("fastly"))
        assertEquals(HolidaySource.GITHUB_RAW, HolidaySource.fromCode("github_raw"))
        assertEquals(HolidaySource.CUSTOM, HolidaySource.fromCode("custom"))
    }

    @Test
    fun testCustomUrlNormalization() {
        assertEquals(
            "https://ghproxy.net/https://raw.githubusercontent.com/irvingwu0v0/holiday-cn/master",
            HolidaySource.normalizeCustomMirrorUrl("https://ghproxy.net"),
        )
        assertEquals(
            "https://ghproxy.net/https://raw.githubusercontent.com/irvingwu0v0/holiday-cn/master",
            HolidaySource.normalizeCustomMirrorUrl("https://ghproxy.net/https://raw.githubusercontent.com/irvingwu0v0/holiday-cn/master/"),
        )
        assertEquals(
            "https://raw.githubusercontent.com/irvingwu0v0/holiday-cn/master",
            HolidaySource.normalizeCustomMirrorUrl("https://raw.githubusercontent.com"),
        )
        assertEquals("", HolidaySource.normalizeCustomMirrorUrl(null))
        assertEquals("", HolidaySource.normalizeCustomMirrorUrl("  "))
    }

    @Test
    fun testGhproxyFetch() {
        val client = HolidayClient()
        val data = client.fetch(2026, HolidayRegion.XINJIANG, HolidaySource.GHPROXY)
        assertEquals(2026, data.year)
        assertTrue(data.days.isNotEmpty())
        assertTrue("数据来源应标明 ghproxy 加速：${data.source}", data.source.contains("ghproxy"))
        val hasRouzi = data.days.values.any { it.name.contains("肉孜节") }
        assertTrue("ghproxy 数据源应能获取肉孜节", hasRouzi)
    }

    @Test
    fun testJsdelivrFetch() {
        val client = HolidayClient()
        val data = client.fetch(2026, HolidayRegion.NONE, HolidaySource.JSDELIVR)
        assertEquals(2026, data.year)
        assertTrue(data.days.isNotEmpty())
        assertTrue("数据来源应标明 jsDelivr：${data.source}", data.source.contains("jsDelivr"))
    }

    @Test
    fun testResolveYearsToFetchDynamic() {
        val client = HolidayClient()

        // 1. 普通年中：只包含当前年份，不硬编码 2026
        val normalYears = client.resolveYearsToFetch(today = LocalDate.of(2028, 5, 15))
        assertEquals(setOf(2028), normalYears)

        // 2. 跨年学期：秋季开学跨至次年
        val crossTermYears = client.resolveYearsToFetch(
            today = LocalDate.of(2027, 9, 1),
            termStart = LocalDate.of(2027, 9, 1),
            maxWeeks = 25,
        )
        assertTrue(crossTermYears.contains(2027))
        assertTrue(crossTermYears.contains(2028))

        // 3. 从学期字符串动态解析
        val termNameYears = client.resolveYearsToFetch(
            today = LocalDate.of(2029, 6, 1),
            termName = "2029-2030-1",
        )
        assertTrue(termNameYears.contains(2029))
        assertTrue(termNameYears.contains(2030))

        // 4. 年末第四季度自动包含次年
        val lateYears = client.resolveYearsToFetch(today = LocalDate.of(2027, 10, 15))
        assertTrue(lateYears.contains(2027))
        assertTrue(lateYears.contains(2028))

        // 5. 年初第一季度包含上一年
        val earlyYears = client.resolveYearsToFetch(today = LocalDate.of(2029, 1, 10))
        assertTrue(earlyYears.contains(2029))
        assertTrue(earlyYears.contains(2028))
    }
}
