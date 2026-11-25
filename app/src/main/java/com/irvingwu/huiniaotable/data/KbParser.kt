package com.irvingwu.huiniaotable.data

import com.irvingwu.huiniaotable.t
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class ParseException(message: String) : Exception(message)

/** 教务系统页面族。解析策略不同，但最终都产出同一套 [Course]。 */
enum class SchoolFamily {
    Qiangzhi,
    Zhengfang,
    Kingosoft,
    Generic,
}

/**
 * 解析高校教务系统「学期理论课表」页面。
 *
 * 不需要用户选择学校：按页面特征识别强智 / 正方 / 青果，
 * 其余结构用通用表格 + 文本行启发式解析。
 *
 * 强智页面结构：
 * ```
 * <table id="timetable">
 *   <tr><th></th><th>星期一</th>…<th>星期日</th></tr>
 *   <tr>
 *     <th>第1-2节</th>
 *     <td>
 *       <div id="HASH-1-1" class="kbcontent1">课程名<br/>(教学班)<br/>
 *            <font title='周次(节次)'>2-18(周)</font><br/><font title='教室'>第一教学楼501</font></div>
 *     </td>…
 *   </tr>
 * </table>
 * ```
 *
 * 正方 / 青果的格子则多为「课程名 / 周次 / 教师 / 地点」这类纯文本行，
 * 用 [parseGenericBlock] 统一提取。
 */
object KbParser {

    data class Term(val value: String, val label: String, val selected: Boolean)

    data class Parsed(val schedule: Schedule, val terms: List<Term>)

    private val BR = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)
    private val RANGE = Regex("^(\\d+)\\s*[-–~～至]\\s*(\\d+)$")
    private val SLOT_RANGE = Regex("\\[(\\d+)\\s*[-–]\\s*(\\d+)节\\]")
    private val SLOT_SINGLE = Regex("\\[(\\d+)节\\]")
    private val SLOT_LOOSE = Regex("(\\d+)\\s*[-–~]\\s*(\\d+)\\s*节")
    private val SLOT_LOOSE_SINGLE = Regex("第\\s*(\\d+)\\s*节")
    private val ROW_SLOTS = Regex("第\\s*(\\d+)\\s*[-–~]\\s*(\\d+)\\s*节")
    private val ROW_SLOTS_COMMA = Regex("第?\\s*(\\d+)\\s*[,，、]\\s*(\\d+)\\s*节")
    private val ROW_SLOT_SINGLE = Regex("第\\s*(\\d+)\\s*节")
    private val ROW_BIG = Regex("第?\\s*([一二三四五六七八九十]+)\\s*大节")
    private val DAY = Regex("(?:星期|周)([一二三四五六日天])")
    private val WEEK_LINE = Regex("\\d")
    private val TEACHER_LINE = Regex("^[\\u4e00-\\u9fa5]{2,4}([、,，/]\\s*[\\u4e00-\\u9fa5]{2,4})*$")
    private val ROOM_HINT = Regex("楼|室|馆|场|机房|实验|中心|校区|号|#|\\d")
    private val CLASS_LABEL = Regex("^[(（].*[)）]$")

    /** 只做特征识别，供网页导入界面显示当前识别到的教务系统。 */
    fun detectFamily(html: String): SchoolFamily = detectFamily(Jsoup.parse(html))

    private fun detectFamily(doc: Document): SchoolFamily {
        val raw = doc.outerHtml()
        return when {
            doc.selectFirst("div.kbcontent, div.kbcontent1") != null -> SchoolFamily.Qiangzhi
            doc.selectFirst("table#timetable") != null && raw.contains("kbcontent") -> SchoolFamily.Qiangzhi
            doc.selectFirst("table#kbtable") != null -> SchoolFamily.Zhengfang
            raw.contains("jwglxt") || raw.contains("SafeCodeImg") -> SchoolFamily.Zhengfang
            doc.selectFirst("table#Table1") != null -> SchoolFamily.Kingosoft
            raw.contains("kingosoft") || raw.contains("/xsd/") -> SchoolFamily.Kingosoft
            else -> SchoolFamily.Generic
        }
    }

    fun parse(html: String, fallbackTerm: String = ""): Parsed {
        val doc = Jsoup.parse(html)
        val family = detectFamily(doc)
        val terms = parseTerms(doc)
        val selectedTerm = terms.firstOrNull { it.selected } ?: terms.firstOrNull()

        val table = pickTable(doc)
            ?: throw ParseException(t("页面里没有找到课表表格", "No timetable table was found on the page"))

        val trs = table.select("tr")
        if (trs.isEmpty()) throw ParseException(t("课表表格是空的", "The timetable table is empty"))

        val headerRow = trs.firstOrNull { it.text().contains("星期") || it.text().contains("周一") }
            ?: trs.firstOrNull()
            ?: throw ParseException(t("课表表格是空的", "The timetable table is empty"))
        val colToDay = buildDayColumns(headerRow)

        val slotRows = mutableListOf<SlotRow>()
        val courses = mutableListOf<Course>()
        for (tr in trs) {
            val cells = tr.children().filter { it.tagName() == "th" || it.tagName() == "td" }
            if (cells.isEmpty()) continue
            val head = cells.first()
            if (head.tagName() != "th" && head.parent() !== tr) continue
            val slots = parseRowSlots(head.text()) ?: continue
            val rowIndex = slotRows.size
            slotRows += SlotRow(
                head.text().trim().ifBlank {
                    t("第${slots.first}-${slots.second}节", "Periods ${slots.first}–${slots.second}")
                },
                slots.first,
                slots.second,
            )

            var column = 0
            for (cell in cells) {
                val span = cell.attr("colspan").toIntOrNull()?.coerceAtLeast(1) ?: 1
                if (cell !== head) {
                    val day = colToDay.getOrElse(column) { 0 }
                    if (day in 1..7) {
                        courses += coursesInCell(cell, day, rowIndex, slots.first, slots.second, family)
                    }
                }
                column += span
            }
        }

        val unscheduled = parseUnscheduled(doc)
        if (courses.isEmpty() && unscheduled.isEmpty() && !html.contains("kbcontent")) {
            throw ParseException(
                t(
                    "课表页面结构无法识别（可能是登录状态失效或页面已改版）",
                    "The timetable page could not be parsed. Your session may have expired or the page changed.",
                ),
            )
        }

        val merged = coalesce(courses)
        val (maxWeek, lastTeachingWeek) = weekRange(doc, merged)
        val schedule = Schedule(
            term = selectedTerm?.value ?: fallbackTerm,
            termLabel = selectedTerm?.label ?: fallbackTerm,
            courses = merged,
            unscheduled = unscheduled,
            slotRows = slotRows.ifEmpty { SlotRow.DEFAULT },
            maxWeek = maxWeek,
            lastTeachingWeek = lastTeachingWeek,
            fetchedAt = System.currentTimeMillis(),
        )
        return Parsed(schedule, terms)
    }

    /** 课表表格：按常见 id / class 依次尝试，只接受含「星期」字样的表格。 */
    private fun pickTable(doc: Document): Element? {
        val selectors = listOf(
            "table#timetable",
            "table#kbtable",
            "table#Table1",
            "table#table1",
            "table.kb",
            "table.timetable",
            "table",
        )
        for (selector in selectors) {
            val found = doc.select(selector).firstOrNull { table ->
                val text = table.text()
                (text.contains("星期") || text.contains("周一")) && table.select("tr").size > 1
            }
            if (found != null) return found
        }
        return null
    }

    /** 表头「星期一…星期日」占用的列号。返回下标 = 列号，值 = 星期几（0 表示不是星期列）。 */
    private fun buildDayColumns(headerRow: Element): List<Int> {
        val out = mutableListOf<Int>()
        for (cell in headerRow.children()) {
            val span = cell.attr("colspan").toIntOrNull()?.coerceAtLeast(1) ?: 1
            val day = dayOf(cell.text())
            repeat(span) { out += (if (it == 0) day else 0) }
        }
        return out
    }

    private fun dayOf(text: String): Int = when (DAY.find(text.replace(" ", ""))?.groupValues?.get(1)) {
        "一" -> 1; "二" -> 2; "三" -> 3; "四" -> 4; "五" -> 5; "六" -> 6; "日", "天" -> 7; else -> 0
    }

    private fun parseRowSlots(text: String): Pair<Int, Int>? {
        val clean = text.replace(" ", "").replace("\u00a0", "")
        ROW_SLOTS.find(clean)?.let {
            return it.groupValues[1].toIntOrNull()?.let { a ->
                it.groupValues[2].toIntOrNull()?.let { b -> a to b }
            }
        }
        ROW_SLOTS_COMMA.find(clean)?.let {
            return it.groupValues[1].toIntOrNull()?.let { a ->
                it.groupValues[2].toIntOrNull()?.let { b -> a to b }
            }
        }
        ROW_SLOT_SINGLE.find(clean)?.let {
            val a = it.groupValues[1].toIntOrNull() ?: return null
            return a to a
        }
        ROW_BIG.find(clean)?.let {
            val n = chineseNumber(it.groupValues[1]) ?: return null
            return (n - 1) * 2 + 1 to n * 2
        }
        return null
    }

    private fun chineseNumber(raw: String): Int? = when (raw) {
        "一" -> 1; "二" -> 2; "三" -> 3; "四" -> 4; "五" -> 5
        "六" -> 6; "七" -> 7; "八" -> 8; "九" -> 9; "十" -> 10
        else -> null
    }

    private fun coursesInCell(
        cell: Element,
        day: Int,
        rowIndex: Int,
        rowStart: Int,
        rowEnd: Int,
        family: SchoolFamily,
    ): List<Course> {
        if (family == SchoolFamily.Qiangzhi) {
            val parsed = qiangzhiCoursesInCell(cell, day, rowIndex, rowStart, rowEnd)
            if (parsed.isNotEmpty()) return parsed
        }
        return cellBlocks(cell).mapNotNull { block ->
            parseGenericBlock(block, day, rowIndex, rowStart, rowEnd)
        }
    }

    private fun qiangzhiCoursesInCell(
        cell: Element,
        day: Int,
        rowIndex: Int,
        rowStart: Int,
        rowEnd: Int,
    ): List<Course> {
        val out = mutableListOf<Course>()
        var summaries = cell.select("div.kbcontent1")
        if (summaries.isEmpty()) summaries = cell.select("div.kbcontent")
        val allDivs = cell.select("div")

        for (summary in summaries) {
            if (summary.text().isBlank()) continue
            val (name, classLabel) = splitNameAndClass(summary)
            if (name.isBlank()) continue

            val id = summary.id()
            // 摘要块 id 形如 HASH-星期-1，详情块是 HASH-星期-2；配不上时再按课程名匹配，
            // 绝不用「随便一个详情块」，否则会把同格子另一门课的教师认错。
            val byId = if (id.endsWith("-1")) {
                allDivs.firstOrNull { it.id() == id.dropLast(1) + "2" && it !== summary }
            } else {
                null
            }
            val detail = byId ?: allDivs.firstOrNull {
                it !== summary &&
                    it.classNames().contains("kbcontent") &&
                    it.text().trimStart().startsWith(name)
            }

            // 周次以界面上可见的摘要为准（含「单/双」这类标记），详情块只补教师、
            // 教室和精确节次。
            val summaryWeekText = summary.selectFirst("font[title*=周次]")?.text().orEmpty()
            val detailWeekText = detail?.selectFirst("font[title*=周次]")?.text().orEmpty()
            val weeks = parseWeeks(summaryWeekText).ifEmpty { parseWeeks(detailWeekText) }
            val slots = parseSlots(detailWeekText.ifBlank { summaryWeekText }) ?: (rowStart to rowEnd)

            val teacher = TeacherNames.clean(detail?.selectFirst("font[title*=教师]")?.text())
            val room = (detail?.selectFirst("font[title*=教室]") ?: summary.selectFirst("font[title*=教室]"))
                ?.text()?.trim()?.takeIf { it.isNotBlank() }
            val campus = detail?.selectFirst("font[title*=教学楼]")?.text()?.trim()
                ?.trim('[', ']', '【', '】')?.takeIf { it.isNotBlank() }

            out += Course(
                name = name,
                classLabel = classLabel,
                teacher = teacher,
                room = room,
                campus = campus,
                dayOfWeek = day,
                startSlot = slots.first,
                endSlot = slots.second,
                weeks = weeks,
                rowIndex = rowIndex,
            )
        }
        return out
    }

    /** 把格子拆成若干门课的文本块：优先按 `<hr>`，其次按连续两个 `<br>`。 */
    private fun cellBlocks(cell: Element): List<Element> {
        val html = cell.html().replace("◇", "<br>")
        val parts = html.split(Regex("(?i)<hr[^>]*>"))
        val split = if (parts.size > 1) parts else html.split(Regex("(?i)(?:<br\\s*/?>\\s*){2,}"))
        return split
            .map { Jsoup.parseBodyFragment(it).body() }
            .filter { it.text().isNotBlank() }
    }

    /** 通用文本块解析：课程名 / 周次 / 教师 / 地点，顺序按行启发式判断。 */
    private fun parseGenericBlock(
        block: Element,
        day: Int,
        rowIndex: Int,
        rowStart: Int,
        rowEnd: Int,
    ): Course? {
        val lines = block.html().split(BR)
            .map { Jsoup.parse(it).text().replace("\u00a0", " ").trim() }
            .filter { it.isNotEmpty() }
        if (lines.isEmpty()) return null

        val joined = lines.joinToString(" ")
        val weeks = weeksFromLines(lines)
        val slots = parseSlots(joined) ?: (rowStart to rowEnd)

        val rest = lines.filterNot { isMetaLine(it) }
        if (rest.isEmpty()) return null

        val name = cleanCourseName(rest.first())
        if (name.isBlank()) return null

        var classLabel: String? = null
        var teacher: String? = null
        var room: String? = null
        var campus: String? = null
        for (line in rest.drop(1)) {
            when {
                classLabel == null && CLASS_LABEL.matches(line) -> classLabel = line.cleanClassLabel()
                teacher == null && TEACHER_LINE.matches(line) -> teacher = TeacherNames.clean(line)
                room == null && ROOM_HINT.containsMatchIn(line) -> room = line.trim('[', ']', '【', '】')
                room == null -> room = line
                campus == null -> campus = line.trim('[', ']', '【', '】')
            }
        }

        return Course(
            name = name,
            classLabel = classLabel,
            teacher = teacher,
            room = room,
            campus = campus,
            dayOfWeek = day,
            startSlot = slots.first,
            endSlot = slots.second,
            weeks = weeks,
            rowIndex = rowIndex,
        )
    }

    private fun isWeekLine(line: String): Boolean {
        val compact = line.replace(" ", "")
        if (!compact.contains("周")) return false
        return WEEK_LINE.containsMatchIn(compact) || compact.contains("单") || compact.contains("双")
    }

    private fun weeksFromLines(lines: List<String>): List<Int> =
        lines.filter { isWeekLine(it) }
            .flatMap { parseWeeks(it) }
            .distinct()
            .sorted()

    private fun isMetaLine(line: String): Boolean {
        val compact = line.replace(" ", "")
        if (isWeekLine(line)) return true
        if (Regex("^第?\\d+[-–~]\\d+节$").matches(compact)) return true
        if (Regex("^\\[?\\d+节\\]?$").matches(compact)) return true
        if (Regex("^\\[?\\d+[-–]\\d+节\\]?$").matches(compact)) return true
        return false
    }

    private fun cleanCourseName(raw: String): String {
        var name = raw.trim()
        name = name.replace(Regex("[（(]\\s*\\d+(?:[-–~,]\\d+)*\\s*周?[^)）]*[)）]"), "").trim()
        name = name.replace(Regex("\\s+"), " ")
        return name
    }

    /** 去掉 <font> 之后剩下的文本行：第一行课程名，第二行通常是教学班，并剥离括号。 */
    private fun splitNameAndClass(div: Element): Pair<String, String?> {
        val clone = div.clone()
        clone.select("font").remove()
        val lines = clone.html().split(BR)
            .map { Jsoup.parse(it).text().trim() }
            .filter { it.isNotEmpty() }
        val name = lines.firstOrNull().orEmpty()
        val rawClass = lines.drop(1).firstOrNull { it.startsWith("(") || it.startsWith("（") }
            ?: lines.getOrNull(1)?.takeIf { it.length <= 24 }
        val cleanClass = rawClass?.cleanClassLabel()?.takeIf { it.isNotBlank() }
        return name to cleanClass
    }

    /**
     * 同一门课如果被教务系统拆在相邻大节（1-2 节 + 3 节），合并成一条，
     * 这样三节课的课在界面上是一个连续方块。
     */
    private fun coalesce(courses: List<Course>): List<Course> {
        val grouped = courses.groupBy {
            listOf(
                it.name, it.classLabel.orEmpty(), it.teacher.orEmpty(), it.room.orEmpty(),
                it.campus.orEmpty(), it.dayOfWeek.toString(), it.weeks.sorted().joinToString(","),
            ).joinToString("|")
        }
        val out = mutableListOf<Course>()
        for (list in grouped.values) {
            var current: Course? = null
            for (course in list.sortedBy { it.startSlot }) {
                val cur = current
                if (cur != null && course.startSlot <= cur.endSlot + 1) {
                    current = cur.copy(endSlot = maxOf(cur.endSlot, course.endSlot))
                } else {
                    cur?.let { out += it }
                    current = course
                }
            }
            current?.let { out += it }
        }
        return out.sortedWith(compareBy({ it.dayOfWeek }, { it.startSlot }))
    }

    /**
     * 解析周次文本，支持 "2-18(周)"、"2,4,6,8(周)"、"1-16(周)[01-02节]"、含「单/双」的写法。
     * 周次是课程的固有属性，与查看第几周无关。
     */
    fun parseWeeks(raw: String): List<Int> {
        if (raw.isBlank()) return emptyList()
        val odd = raw.contains("单")
        val even = raw.contains("双")
        var text = raw.substringBefore('[').substringBefore('【')
        text = text.replace(Regex("[（(][^）)]*[）)]"), " ")
        text = text.replace("周", " ")
        val weeks = sortedSetOf<Int>()
        for (token in text.split(',', '，', '、', ';', '；', ' ', '\t', '\n')) {
            val part = token.trim()
            if (part.isEmpty()) continue
            val range = RANGE.find(part)
            if (range != null) {
                val a = range.groupValues[1].toIntOrNull() ?: continue
                val b = range.groupValues[2].toIntOrNull() ?: continue
                if (a <= b) for (i in a..b) weeks.add(i) else for (i in b..a) weeks.add(i)
            } else {
                val digits = part.filter { it.isDigit() }
                if (digits.isNotEmpty() && digits.length <= 3) digits.toIntOrNull()?.let { weeks.add(it) }
            }
        }
        val list = weeks.toList()
        return when {
            odd && !even -> list.filter { it % 2 == 1 }
            even && !odd -> list.filter { it % 2 == 0 }
            else -> list
        }
    }

    private fun parseSlots(raw: String): Pair<Int, Int>? {
        SLOT_RANGE.find(raw)?.let {
            val a = it.groupValues[1].toIntOrNull() ?: return@let
            val b = it.groupValues[2].toIntOrNull() ?: return@let
            return a to b
        }
        SLOT_SINGLE.find(raw)?.let {
            val a = it.groupValues[1].toIntOrNull() ?: return@let
            return a to a
        }
        SLOT_LOOSE.find(raw)?.let {
            val a = it.groupValues[1].toIntOrNull() ?: return@let
            val b = it.groupValues[2].toIntOrNull() ?: return@let
            return a to b
        }
        SLOT_LOOSE_SINGLE.find(raw)?.let {
            val a = it.groupValues[1].toIntOrNull() ?: return@let
            return a to a
        }
        return null
    }

    /**
     * 返回 (maxWeek, lastTeachingWeek)。
     *
     * 周次下拉框里通常列满 30 周（教务系统的通用上限），用它当上界是为了让「当前周」
     * 不会被错误地截断；而界面上生成周次选择器时用有课的周次，避免出现一堆空白周。
     */
    private fun weekRange(doc: Document, courses: List<Course>): Pair<Int, Int> {
        val fromSelect = doc.select("select#zc option, select[name=zc] option")
            .mapNotNull { it.attr("value").toIntOrNull() }
            .maxOrNull()
        val fromCourses = courses.flatMap { it.weeks }.maxOrNull()
        val lastTeaching = (fromCourses ?: fromSelect ?: 20).coerceIn(1, 40)
        val max = maxOf(lastTeaching, fromSelect ?: lastTeaching).coerceIn(lastTeaching, 40)
        return max to lastTeaching
    }

    private fun parseTerms(doc: Document): List<Term> =
        doc.select("select#xnxq01id option, select[name=xnxq01id] option, select#xq option, select[name=xq] option")
            .mapNotNull { option ->
                val value = option.attr("value").trim()
                if (value.isEmpty()) null
                else Term(value, option.text().trim().ifBlank { value }, option.hasAttr("selected"))
            }

    private fun parseUnscheduled(doc: Document): List<UnscheduledCourse> {
        val table = doc.selectFirst("table#dataTables") ?: return emptyList()
        val out = mutableListOf<UnscheduledCourse>()
        for (tr in table.select("tr")) {
            val tds = tr.children().filter { it.tagName() == "td" }
            if (tds.size < 4) continue
            val name = tds.getOrNull(2)?.text()?.trim().orEmpty()
            if (name.isBlank()) continue
            out += UnscheduledCourse(
                name = name,
                classLabel = tds.getOrNull(0)?.text()?.trim()?.cleanClassLabel()?.takeIf { it.isNotBlank() },
                code = tds.getOrNull(1)?.text()?.trim()?.takeIf { it.isNotBlank() },
                teacher = TeacherNames.clean(tds.getOrNull(3)?.text()),
                category = tds.getOrNull(6)?.text()?.trim()?.takeIf { it.isNotBlank() },
                attribute = tds.getOrNull(7)?.text()?.trim()?.takeIf { it.isNotBlank() },
            )
        }
        return out
    }
}
