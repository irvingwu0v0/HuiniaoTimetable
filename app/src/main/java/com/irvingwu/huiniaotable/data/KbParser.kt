package com.irvingwu.huiniaotable.data

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class ParseException(message: String) : Exception(message)

/**
 * 解析高校教务系统「学期理论课表」页面。
 *
 * 页面结构：
 * ```
 * <table id="timetable">
 *   <tr><th></th><th>星期一</th>…<th>星期日</th></tr>
 *   <tr>
 *     <th>第1-2节</th>
 *     <td>
 *       <div id="HASH-1-1" class="kbcontent1">课程名<br/>(教学班)<br/>
 *            <font title='周次(节次)'>2-18(周)</font><br/><font title='教室'>第一教学楼501</font></div>
 *       <div id="HASH-1-2" class="kbcontent">课程名<br/>(教学班)<br/>
 *            <font title='教师'>张老师</font><br/>
 *            <font title='周次(节次)'>2-18(周)[01-02节]</font><br/>
 *            <font title='教学楼'>【第一教学楼】</font><font title='教室'>第一教学楼501</font></div>
 *     </td>…
 *   </tr>
 * </table>
 * <table id="dataTables">…无课表课程…</table>
 * ```
 */
object KbParser {

    data class Term(val value: String, val label: String, val selected: Boolean)

    data class Parsed(val schedule: Schedule, val terms: List<Term>)

    private val BR = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)
    private val RANGE = Regex("^(\\d+)\\s*[-–~～至]\\s*(\\d+)$")
    private val SLOT_RANGE = Regex("\\[(\\d+)\\s*[-–]\\s*(\\d+)节\\]")
    private val SLOT_SINGLE = Regex("\\[(\\d+)节\\]")
    private val ROW_SLOTS = Regex("第\\s*(\\d+)\\s*[-–]\\s*(\\d+)\\s*节")
    private val ROW_SLOT_SINGLE = Regex("第\\s*(\\d+)\\s*节")
    private val DAY = Regex("星期([一二三四五六日天])")

    fun parse(html: String, fallbackTerm: String = ""): Parsed {
        val doc = Jsoup.parse(html)
        val terms = parseTerms(doc)
        val selectedTerm = terms.firstOrNull { it.selected } ?: terms.firstOrNull()

        val table = doc.selectFirst("table#timetable")
            ?: doc.selectFirst("table#kbtable")
            ?: doc.selectFirst("table")
            ?: throw ParseException("页面里没有找到课表表格")

        val trs = table.select("tr")
        // jsoup 的 Elements.first() 是可空覆盖，这里显式处理空表。
        val headerRow = trs.firstOrNull() ?: throw ParseException("课表表格是空的")

        val colToDay = buildDayColumns(headerRow)

        val slotRows = mutableListOf<SlotRow>()
        val courses = mutableListOf<Course>()
        for (tr in trs) {
            val cells = tr.children().filter { it.tagName() == "th" || it.tagName() == "td" }
            if (cells.isEmpty()) continue
            val head = cells.first()
            if (head.tagName() != "th") continue
            val slots = parseRowSlots(head.text()) ?: continue
            val rowIndex = slotRows.size
            slotRows += SlotRow(head.text().trim().ifBlank { "第${slots.first}-${slots.second}节" }, slots.first, slots.second)

            var column = 0
            for (cell in cells) {
                val span = cell.attr("colspan").toIntOrNull()?.coerceAtLeast(1) ?: 1
                if (cell !== head) {
                    val day = colToDay.getOrElse(column) { 0 }
                    if (day in 1..7) {
                        courses += coursesInCell(cell, day, rowIndex, slots.first, slots.second)
                    }
                }
                column += span
            }
        }

        val unscheduled = parseUnscheduled(doc)
        if (courses.isEmpty() && unscheduled.isEmpty() && !html.contains("kbcontent")) {
            throw ParseException("课表页面结构无法识别（可能是登录状态失效或页面已改版）")
        }

        val (maxWeek, lastTeachingWeek) = weekRange(doc, courses)
        val schedule = Schedule(
            term = selectedTerm?.value ?: fallbackTerm,
            termLabel = selectedTerm?.label ?: fallbackTerm,
            courses = courses.distinctBy { it.key },
            unscheduled = unscheduled,
            slotRows = slotRows.ifEmpty { SlotRow.DEFAULT },
            maxWeek = maxWeek,
            lastTeachingWeek = lastTeachingWeek,
            fetchedAt = System.currentTimeMillis(),
        )
        return Parsed(schedule, terms)
    }

    /** 表头「星期一…星期日」占用的列号。返回下标 = 列号，值 = 星期几（0 表示不是星期列）。 */
    private fun buildDayColumns(headerRow: Element): List<Int> {
        val out = mutableListOf<Int>()
        var column = 0
        for (cell in headerRow.children()) {
            val span = cell.attr("colspan").toIntOrNull()?.coerceAtLeast(1) ?: 1
            val day = dayOf(cell.text())
            repeat(span) {
                out += (if (it == 0) day else 0)
                column++
            }
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
        ROW_SLOT_SINGLE.find(clean)?.let {
            val a = it.groupValues[1].toIntOrNull() ?: return null
            return a to a
        }
        return null
    }

    private fun coursesInCell(cell: Element, day: Int, rowIndex: Int, rowStart: Int, rowEnd: Int): List<Course> {
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
        doc.select("select#xnxq01id option, select[name=xnxq01id] option").mapNotNull { option ->
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
