package com.irvingwu.huiniaotable.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** 一个法定假日或调休上班日。 */
data class HolidayDay(
    val date: LocalDate,
    val name: String,
    val isOffDay: Boolean,
)

/** 某一年的放假/调休安排。 */
data class HolidayYear(
    val year: Int,
    val days: Map<LocalDate, HolidayDay>,
    val source: String,
    val fetchedAt: Long,
) {
    fun dayOn(date: LocalDate): HolidayDay? = days[date]

    fun isOffDay(date: LocalDate): Boolean = days[date]?.isOffDay == true

    /** 调休上班日（周末但要上班/上课）。 */
    fun isMakeupWorkday(date: LocalDate): Boolean = days[date]?.isOffDay == false

    fun toJson(): JSONObject = JSONObject().apply {
        put("year", year)
        put("source", source)
        put("fetchedAt", fetchedAt)
        put("days", JSONArray().apply {
            days.values.sortedBy { it.date }.forEach { day ->
                put(
                    JSONObject()
                        .put("date", day.date.toString())
                        .put("name", day.name)
                        .put("off", day.isOffDay),
                )
            }
        })
    }

    companion object {
        fun parse(json: String?): HolidayYear? = try {
            val o = json?.takeIf { it.isNotBlank() }?.let { JSONObject(it) } ?: return null
            val map = linkedMapOf<LocalDate, HolidayDay>()
            o.optJSONArray("days")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val d = arr.optJSONObject(i) ?: continue
                    val date = runCatching { LocalDate.parse(d.optString("date")) }.getOrNull() ?: continue
                    map[date] = HolidayDay(date, d.optString("name"), d.optBoolean("off", true))
                }
            }
            HolidayYear(
                year = o.optInt("year"),
                days = map,
                source = o.optString("source"),
                fetchedAt = o.optLong("fetchedAt"),
            )
        } catch (t: Throwable) {
            null
        }
    }
}

/** 用户手动添加的假期/调休（学校校庆、运动会调休等）。 */
data class HolidayOverride(val date: LocalDate, val name: String, val isOffDay: Boolean) {
    fun toJson(): JSONObject = JSONObject()
        .put("date", date.toString())
        .put("name", name)
        .put("off", isOffDay)

    companion object {
        fun parseList(json: String?): List<HolidayOverride> = try {
            val arr = json?.takeIf { it.isNotBlank() }?.let { JSONArray(it) } ?: JSONArray()
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val date = runCatching { LocalDate.parse(o.optString("date")) }.getOrNull() ?: return@mapNotNull null
                HolidayOverride(date, o.optString("name"), o.optBoolean("off", true))
            }
        } catch (t: Throwable) {
            emptyList()
        }

        fun listToJson(list: List<HolidayOverride>): String =
            JSONArray().apply { list.forEach { put(it.toJson()) } }.toString()
    }
}

/** 把全国数据和用户手动添加的合并成一份日历，手动添加的优先。 */
fun mergeHolidays(
    base: HolidayYear?,
    overrides: List<HolidayOverride>,
    year: Int,
): HolidayYear? {
    if (base == null && overrides.isEmpty()) return null
    val days = LinkedHashMap<LocalDate, HolidayDay>()
    base?.days?.let { days.putAll(it) }
    overrides.forEach { days[it.date] = HolidayDay(it.date, it.name.ifBlank { "自定义" }, it.isOffDay) }
    return HolidayYear(
        year = year,
        days = days,
        source = base?.source ?: "手动添加",
        fetchedAt = base?.fetchedAt ?: 0L,
    )
}

/**
 * 少数民族自治区节假日配置选项。
 * 数据源：https://github.com/irvingwu0v0/holiday-cn
 */
enum class HolidayRegion(val code: String, val displayName: String) {
    NONE("", "国务院公布方案"),
    XINJIANG("xinjiang", "新疆维吾尔自治区"),
    XIZANG("xizang", "西藏自治区"),
    NINGXIA("ningxia", "宁夏回族自治区"),
    GUANGXI("guangxi", "广西壮族自治区"),
    NEIMENGGU("neimenggu", "内蒙古自治区");

    companion object {
        fun fromCode(code: String?): HolidayRegion =
            entries.firstOrNull { it.code.equals(code?.trim(), ignoreCase = true) } ?: NONE
    }
}

/**
 * 节假日数据同步源及镜像节点选项。
 *
 * 支持自动优选、国内 ghproxy 代理加速、jsDelivr CDN、Fastly CDN、GitHub Raw 直连以及自定义镜像。
 */
enum class HolidaySource(
    val code: String,
    val displayName: String,
    val description: String,
) {
    AUTO("auto", "自动优选", "多源轮询容灾（推荐）"),
    GHPROXY("ghproxy", "ghproxy 代理加速", "国内 ghproxy 镜像代理访问 GitHub"),
    JSDELIVR("jsdelivr", "jsDelivr CDN", "全球开源公共 CDN 加速节点"),
    FASTLY("fastly", "Fastly CDN", "Fastly 全球边缘网络分发"),
    GITHUB_RAW("github_raw", "GitHub Raw 直连", "官方源站直连（需良好网络）"),
    CUSTOM("custom", "自定义镜像 / 代理", "指定自定义 ghproxy 或自建源");

    fun mirrors(customBaseUrl: String? = null): List<String> = when (this) {
        AUTO -> listOf(
            "https://ghproxy.net/https://raw.githubusercontent.com/irvingwu0v0/holiday-cn/master",
            "https://cdn.jsdelivr.net/gh/irvingwu0v0/holiday-cn@master",
            "https://fastly.jsdelivr.net/gh/irvingwu0v0/holiday-cn@master",
            "https://gh-proxy.com/https://raw.githubusercontent.com/irvingwu0v0/holiday-cn/master",
            "https://ghfast.top/https://raw.githubusercontent.com/irvingwu0v0/holiday-cn/master",
            "https://raw.githubusercontent.com/irvingwu0v0/holiday-cn/master",
        )
        GHPROXY -> listOf(
            "https://ghproxy.net/https://raw.githubusercontent.com/irvingwu0v0/holiday-cn/master",
            "https://gh-proxy.com/https://raw.githubusercontent.com/irvingwu0v0/holiday-cn/master",
            "https://ghfast.top/https://raw.githubusercontent.com/irvingwu0v0/holiday-cn/master",
        )
        JSDELIVR -> listOf(
            "https://cdn.jsdelivr.net/gh/irvingwu0v0/holiday-cn@master",
        )
        FASTLY -> listOf(
            "https://fastly.jsdelivr.net/gh/irvingwu0v0/holiday-cn@master",
        )
        GITHUB_RAW -> listOf(
            "https://raw.githubusercontent.com/irvingwu0v0/holiday-cn/master",
        )
        CUSTOM -> {
            val normalized = normalizeCustomMirrorUrl(customBaseUrl)
            if (normalized.isNotBlank()) {
                listOf(normalized)
            } else {
                listOf("https://ghproxy.net/https://raw.githubusercontent.com/irvingwu0v0/holiday-cn/master")
            }
        }
    }

    companion object {
        fun fromCode(code: String?): HolidaySource =
            entries.firstOrNull { it.code.equals(code?.trim(), ignoreCase = true) } ?: AUTO

        fun normalizeCustomMirrorUrl(input: String?): String {
            val trimmed = input?.trim()?.removeSuffix("/") ?: return ""
            if (trimmed.isBlank()) return ""
            return if (!trimmed.endsWith("/holiday-cn/master") && !trimmed.endsWith(".json")) {
                if (trimmed.endsWith("https://raw.githubusercontent.com") || trimmed.endsWith("http://raw.githubusercontent.com")) {
                    "$trimmed/irvingwu0v0/holiday-cn/master"
                } else {
                    "$trimmed/https://raw.githubusercontent.com/irvingwu0v0/holiday-cn/master"
                }
            } else {
                trimmed
            }
        }
    }
}

/**
 * 中国法定节假日及少数民族自治区放假/调休数据。
 *
 * 数据来源：https://github.com/irvingwu0v0/holiday-cn
 * 基于官方公告自动检索，支持全国统一法定节假日及五个少数民族自治区（新疆、西藏、宁夏、广西、内蒙古）
 * 特色节假日放假安排（如肉孜节、古尔邦节、藏历新年、壮族三月三等）。
 */
class HolidayClient(private val http: HttpClient = HttpClient()) {

    /**
     * 动态计算需要获取法定节假日数据的公历年份。
     *
     * 判定规则：
     * 1. 始终包含当前日期所在的公历年份 [today.year]；
     * 2. 若传入学期基准首周日期 [termStart]，自动解析开学年份，并依据教学周期（通常 20~25 周）动态推算是否跨至次年；
     * 3. 若传入显式学期年份 [termYear]，自动纳入；
     * 4. 若传入学期名称（如 "2026-2027-1"），自动通过正则提取其中包含的所有 4 位公历年份；
     * 5. 跨年自适应：
     *    - 每年 10 月及以后进入第四季度，中国国务院陆续公布次年法定放假安排，自动尝试获取 [today.year + 1]；
     *    - 每年 1~2 月处于寒假及学期交替期，高校秋季学期通常延续自上年，适度获取 [today.year - 1]。
     */
    fun resolveYearsToFetch(
        today: LocalDate = LocalDate.now(),
        termStart: LocalDate? = null,
        termName: String? = null,
        termYear: Int? = null,
        maxWeeks: Int = 25,
    ): Set<Int> {
        val years = linkedSetOf<Int>()
        years.add(today.year)

        if (termStart != null) {
            years.add(termStart.year)
            years.add(termStart.plusWeeks(maxWeeks.toLong()).year)
        }

        termYear?.let { years.add(it) }

        if (!termName.isNullOrBlank()) {
            val yearPattern = Regex("""\b(20\d\d)\b""")
            yearPattern.findAll(termName).forEach { match ->
                match.value.toIntOrNull()?.let { years.add(it) }
            }
        }

        if (today.monthValue >= 10) {
            years.add(today.year + 1)
        }
        if (today.monthValue <= 2) {
            years.add(today.year - 1)
        }

        return years
    }

    /**
     * 多年合并数据：根据学期跨度及当前公历年份动态加载所有相关年份的法定假日与调休安排。
     */
    fun fetchMerged(
        today: LocalDate = LocalDate.now(),
        region: HolidayRegion = HolidayRegion.NONE,
        source: HolidaySource = HolidaySource.AUTO,
        customBaseUrl: String? = null,
        termStart: LocalDate? = null,
        termName: String? = null,
        termYear: Int? = null,
        targetYears: Collection<Int>? = null,
    ): HolidayYear {
        val yearsToFetch = targetYears?.toSet() ?: resolveYearsToFetch(
            today = today,
            termStart = termStart,
            termName = termName,
            termYear = termYear,
        )

        val allDays = LinkedHashMap<LocalDate, HolidayDay>()
        var primarySource: String? = null

        for (y in yearsToFetch) {
            val yrData = runCatching { fetch(y, region, source, customBaseUrl) }.getOrNull()
            if (yrData != null && yrData.days.isNotEmpty()) {
                allDays.putAll(yrData.days)
                if (primarySource == null || (region != HolidayRegion.NONE && yrData.source.contains(region.displayName))) {
                    primarySource = yrData.source
                }
            }
        }

        if (allDays.isEmpty()) {
            return fetch(today.year, region, source, customBaseUrl)
        }

        val representativeYear = termStart?.year ?: termYear ?: today.year
        return HolidayYear(
            year = representativeYear,
            days = allDays,
            source = primarySource ?: "holiday-cn（${source.displayName}）",
            fetchedAt = System.currentTimeMillis(),
        )
    }

    fun fetch(
        year: Int,
        region: HolidayRegion = HolidayRegion.NONE,
        source: HolidaySource = HolidaySource.AUTO,
        customBaseUrl: String? = null,
    ): HolidayYear {
        val mirrors = source.mirrors(customBaseUrl)
        var lastError: Throwable? = null
        if (region != HolidayRegion.NONE && region.code.isNotBlank()) {
            for (base in mirrors) {
                try {
                    return fetchHolidayCn(base, year, region)
                } catch (t: Throwable) {
                    lastError = t
                }
            }
        }
        for (base in mirrors) {
            try {
                return fetchHolidayCn(base, year, HolidayRegion.NONE)
            } catch (t: Throwable) {
                lastError = t
            }
        }

        throw ParseException(lastError?.message ?: "调休数据获取失败，请检查网络或切换数据源")
    }

    private fun fetchHolidayCn(baseUrl: String, year: Int, region: HolidayRegion): HolidayYear {
        val filename = if (region != HolidayRegion.NONE && region.code.isNotBlank()) {
            "${year}_${region.code}.json"
        } else {
            "$year.json"
        }
        val res = http.get("$baseUrl/$filename")
        if (res.code != 200) throw ParseException("HTTP ${res.code}: $baseUrl/$filename")
        val root = JSONObject(res.body)
        val days = linkedMapOf<LocalDate, HolidayDay>()
        val arr = root.optJSONArray("days") ?: JSONArray()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val date = runCatching { LocalDate.parse(o.optString("date")) }.getOrNull() ?: continue
            days[date] = HolidayDay(date, o.optString("name"), o.optBoolean("isOffDay", true))
        }
        // 下一年的文件会提前存在但 days 为空，这里按「还没发布」处理，避免覆盖已有缓存。
        if (days.isEmpty()) throw ParseException("$year 年的放假通知还没发布")
        val mirrorTag = when {
            baseUrl.contains("ghproxy") || baseUrl.contains("gh-proxy") || baseUrl.contains("ghfast") -> "ghproxy 加速"
            baseUrl.contains("jsdelivr") && !baseUrl.contains("fastly") -> "jsDelivr CDN"
            baseUrl.contains("fastly") -> "Fastly CDN"
            baseUrl.contains("raw.githubusercontent.com") -> "GitHub Raw"
            else -> "自定义节点"
        }
        val sourceDesc = if (region != HolidayRegion.NONE && region.code.isNotBlank()) {
            "holiday-cn（${region.displayName} · $mirrorTag）"
        } else {
            "holiday-cn（国务院公布方案 · $mirrorTag）"
        }
        return HolidayYear(year, days, sourceDesc, System.currentTimeMillis())
    }
}
