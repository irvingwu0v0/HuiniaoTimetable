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

        val tables = doc.select("table").sortedByDescending { it.id().lowercase() in setOf("kbtable", "table1", "timetable") }
        val (slotRows, courses) = tables.firstNotNullOfOrNull { element ->
            val table = AcademicTable(element)
            parseList(table) ?: parseGrid(table, family) ?: parseAcross(table, family)
        }
            ?: throw ParseException(t("没有识别到课程，请打开学期课表，或导入带星期和节次的 HTML / XLSX 表格。", "No classes found. Open the semester timetable or import an HTML / XLSX table with weekdays and periods."))

        val unscheduled = parseUnscheduled(doc)
        val merged = coalesce(courses)
        val (maxWeek, lastTeachingWeek) = weekRange(doc, merged)
        val schedule = Schedule(
            term = selectedTerm?.value ?: fallbackTerm,
            termLabel = selectedTerm?.label ?: fallbackTerm,
            courses = merged,
            unscheduled = unscheduled,
            slotRows = slotRows,
            maxWeek = maxWeek,
            lastTeachingWeek = lastTeachingWeek,
            fetchedAt = System.currentTimeMillis(),
        )
        return Parsed(schedule.byPeriod(), terms)
    }

    private fun parseGrid(table: AcademicTable, family: SchoolFamily): Pair<List<SlotRow>, List<Course>>? {
        val header = table.rows.indexOfFirst { row -> row.values.any { dayOf(it.text()) != 0 } }
        if (header < 0) return null
        val days = table.rows[header].mapValues { dayOf(it.value.text()) }.filterValues { it != 0 }
        val firstDay = days.keys.min()
        val periods = table.rows.drop(header + 1).mapNotNull { cells ->
            val (label, slots) = cells.filterKeys { it < firstDay }.values.firstNotNullOfOrNull { cell ->
                parseRowSlots(cell.text())?.let { cell.text().trim() to it }
            }
                ?: return@mapNotNull null
            SlotRow(label, slots.first, slots.second) to cells
        }
        val courses = buildList {
            periods.forEachIndexed { index, (row, cells) ->
                for ((column, day) in days) {
                    val cell = cells[column] ?: continue
                    if (periods.getOrNull(index - 1)?.second?.get(column) === cell) continue
                    if (cell.selectFirst("table") != null || cell.attr("colspan").let { (it.toIntOrNull() ?: 1) > 1 }) continue
                    var last = index
                    while (periods.getOrNull(last + 1)?.second?.get(column) === cell) last++
                    addAll(coursesInCell(cell, day, index, row.startSlot, periods[last].first.endSlot, family))
                }
            }
        }
        return if (courses.isEmpty()) null else periods.map { it.first } to courses
    }

    private fun parseAcross(table: AcademicTable, family: SchoolFamily): Pair<List<SlotRow>, List<Course>>? {
        val header = table.rows.indexOfFirst { row -> row.values.count { parseRowSlots(it.text()) != null } >= 2 }
        if (header < 0) return null
        val columns = table.rows[header].mapNotNull { (column, cell) -> parseRowSlots(cell.text())?.let { column to it } }.toMap()
        val courses = buildList {
            for (row in table.rows.drop(header + 1)) {
                val day = row.filterKeys { it < columns.keys.min() }.values.map { dayOf(it.text()) }.firstOrNull { it != 0 } ?: continue
                for ((column, slots) in columns) {
                    val cell = row[column] ?: continue
                    if (row[column - 1] === cell || cell.selectFirst("table") != null) continue
                    val last = columns.keys.last { row[it] === cell }
                    addAll(coursesInCell(cell, day, slots.first - 1, slots.first, columns.getValue(last).second, family))
                }
            }
        }
        if (courses.isEmpty()) return null
        return (1..columns.values.maxOf { it.second }).map { SlotRow(it.toString(), it, it) } to courses
    }

    private fun parseList(table: AcademicTable): Pair<List<SlotRow>, List<Course>>? {
        val header = table.rows.indexOfFirst { row -> row.values.any { it.text() in setOf("课程名称", "课程名") } }
        if (header < 0) return null
        val columns = table.rows[header].map { it.value.text().trim() to it.key }.toMap()
        fun column(vararg labels: String) = labels.firstNotNullOfOrNull { columns[it] }
        val name = column("课程名称", "课程名") ?: return null
        val weekday = column("星期", "星期几", "上课星期") ?: return null
        val period = column("节次", "上课节次", "起始节次", "开始节次") ?: return null
        val lastPeriod = column("结束节次", "终止节次")
        val weeks = column("周次", "上课周次", "教学周")
        val teacher = column("授课教师", "任课教师", "教师", "上课教师")
        val room = column("上课地点", "教室", "上课教室", "地点")
        val courses = table.rows.drop(header + 1).flatMap { row ->
            fun text(index: Int?) = index?.let { row[it]?.text()?.trim()?.ifBlank { null } }
            val title = text(name) ?: return@flatMap emptyList()
            val day = text(weekday)?.let { it.toIntOrNull() ?: dayOf(it) } ?: return@flatMap emptyList()
            val rawPeriod = text(period) ?: return@flatMap emptyList()
            if (day !in 1..7) return@flatMap emptyList()
            val periods = text(lastPeriod)?.let { "$rawPeriod-$it" } ?: rawPeriod
            periods.split(',', '，', '、').mapNotNull { part ->
                val slots = parseRowSlots(part) ?: return@mapNotNull null
                Course(title, teacher = TeacherNames.clean(text(teacher)), room = text(room), dayOfWeek = day,
                    startSlot = slots.first, endSlot = slots.second, weeks = parseWeeks(text(weeks).orEmpty()), rowIndex = slots.first - 1)
            }
        }
        val last = courses.maxOfOrNull { it.endSlot } ?: return null
        return (1..last).map { SlotRow(it.toString(), it, it) } to courses
    }

    private fun dayOf(text: String): Int = when (DAY.find(text.replace(" ", ""))?.groupValues?.get(1) ?: text.trim()) {
        "一" -> 1; "二" -> 2; "三" -> 3; "四" -> 4; "五" -> 5; "六" -> 6; "日", "天" -> 7; else -> 0
    }

    private val periodLabel = Regex("第?(\\d{1,2})(?:[-–~～至,，、](\\d{1,2}))?节?")
    private val chinesePeriod = Regex("第?([一二三四五六七八九十]+)节")

    private fun parseRowSlots(text: String): Pair<Int, Int>? {
        val clean = text.filterNot { it.isWhitespace() }
        ROW_BIG.matchEntire(clean)?.let {
            val number = chineseNumber(it.groupValues[1]) ?: return null
            return (number * 2 - 1) to number * 2
        }
        chinesePeriod.matchEntire(clean)?.let {
            val number = chineseNumber(it.groupValues[1]) ?: return null
            return number to number
        }
        val match = periodLabel.matchEntire(clean) ?: return null
        val start = match.groupValues[1].toInt()
        val end = match.groupValues[2].toIntOrNull() ?: start
        return (start to end).takeIf { start in 1..30 && end in start..30 }
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
        val structured = cell.select(".timetable_con").mapNotNull { block ->
            val name = block.selectFirst(".title")?.text()?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            fun field(title: String) = block.selectFirst("[title*=$title]")?.parent()?.selectFirst(".timetable-content")?.text()
            val time = field("节/周") ?: field("周次").orEmpty()
            val slots = parseSlots(time) ?: (rowStart to rowEnd)
            Course(name, teacher = TeacherNames.clean(field("教师")), room = field("地点") ?: field("教室"),
                dayOfWeek = day, startSlot = slots.first, endSlot = slots.second, weeks = parseWeeks(time), rowIndex = rowIndex)
        }
        if (structured.isNotEmpty()) return structured
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

            val teacher = TeacherNames.clean((detail ?: summary).selectFirst("font[title*=教师]")?.text())
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
        val lines = block.html().replace(Regex("(?i)</(?:div|p|li)>"), "<br>").split(BR)
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
            val label = line.substringBefore('：').substringBefore(':')
            val value = line.removePrefix(label).trimStart('：', ':', ' ')
            when {
                value.isNotEmpty() && label in setOf("教师", "任课教师", "授课教师") -> teacher = TeacherNames.clean(value)
                value.isNotEmpty() && label in setOf("地点", "上课地点", "教室") -> room = value
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
        val text = raw.replace(Regex("[（(\\[【]?第?\\s*\\d+(?:\\s*[-–~,，]\\s*\\d+)*\\s*节[）)\\]】]?"), " ")
            .replace(Regex("(?:星期|周)[一二三四五六日天]"), " ")
        val groups = Regex("(\\d{1,2}(?:\\s*[-–~～至]\\s*\\d{1,2})?(?:\\s*[,，、]\\s*\\d{1,2}(?:\\s*[-–~～至]\\s*\\d{1,2})?)*)\\s*(?:[(（]?周[)）]?)?\\s*[(（]?([单双])?")
        return buildSet {
            for (group in groups.findAll(text)) {
                for (part in group.groupValues[1].split(',', '，', '、')) {
                    val bounds = part.split(Regex("[-–~～至]")).map { it.trim().toInt() }
                    for (week in minOf(bounds.first(), bounds.last())..maxOf(bounds.first(), bounds.last())) {
                        if (week in 1..40 && when (group.groupValues[2]) {
                            "单" -> week % 2 == 1
                            "双" -> week % 2 == 0
                            else -> true
                        }) add(week)
                    }
                }
            }
        }.sorted()
    }

    private val inlinePeriod = Regex("第?\\s*(\\d{1,2})(?:\\s*[-–~～,，、]\\s*(\\d{1,2}))?\\s*节")

    private fun parseSlots(raw: String): Pair<Int, Int>? = inlinePeriod.find(raw)?.let {
        val start = it.groupValues[1].toInt()
        val end = it.groupValues[2].toIntOrNull() ?: start
        (start to end).takeIf { start in 1..30 && end in start..30 }
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
