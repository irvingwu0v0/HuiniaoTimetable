package com.irvingwu.huiniaotable.data

import org.json.JSONArray
import org.json.JSONObject

/** 星期名称，索引 0 对应星期一。 */
val WEEKDAY_NAMES = listOf("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")

/** 周末列的显示策略。默认「自动」：周六周日没课就不占地方。 */
enum class WeekendMode(val label: String) {
    Auto("自动"),
    Always("显示"),
    Hide("隐藏"),
}

/** 课表网格里的一行（一个「大节」）。 */
data class SlotRow(val label: String, val startSlot: Int, val endSlot: Int) {
    companion object {
        /** 解析失败时的兜底行，覆盖常见 1-10 节。 */
        val DEFAULT = listOf(
            SlotRow("第1-2节", 1, 2),
            SlotRow("第3-4节", 3, 4),
            SlotRow("第5-6节", 5, 6),
            SlotRow("第7-8节", 7, 8),
            SlotRow("第9-10节", 9, 10),
        )
    }
}

/**
 * 一门课在某个星期、某个大节里的一次安排。
 * 同一门课在不同周次段可能被教务系统拆成多条，这是正常的。
 */
data class Course(
    val name: String,
    val classLabel: String? = null,
    val teacher: String? = null,
    val room: String? = null,
    val campus: String? = null,
    val dayOfWeek: Int = 1,
    val startSlot: Int = 1,
    val endSlot: Int = 2,
    val weeks: List<Int> = emptyList(),
    /** 该课程落在课表的第几行（对应 [Schedule.slotRows] 的下标）。 */
    val rowIndex: Int = 0,
) {
    val key: String
        get() = listOf(
            name, classLabel.orEmpty(), teacher.orEmpty(), room.orEmpty(),
            dayOfWeek.toString(), "$startSlot-$endSlot", weeks.joinToString(","),
        ).joinToString("|")

    val slotText: String
        get() = if (startSlot >= endSlot) "第${startSlot}节" else "第${startSlot}-${endSlot}节"

    val weekText: String get() = formatWeeks(weeks)

    val dayText: String get() = WEEKDAY_NAMES.getOrElse(dayOfWeek - 1) { "星期$dayOfWeek" }

    /** 周次解析失败（空）时按「每周都有」处理，避免课程从界面上消失。 */
    fun activeIn(week: Int): Boolean = weeks.isEmpty() || weeks.contains(week)

    fun toJson(): JSONObject = JSONObject().apply {
        put("name", name)
        put("classLabel", classLabel ?: JSONObject.NULL)
        put("teacher", teacher ?: JSONObject.NULL)
        put("room", room ?: JSONObject.NULL)
        put("campus", campus ?: JSONObject.NULL)
        put("day", dayOfWeek)
        put("startSlot", startSlot)
        put("endSlot", endSlot)
        put("rowIndex", rowIndex)
        put("weeks", JSONArray(weeks))
    }

    companion object {
        fun fromJson(o: JSONObject) = Course(
            name = o.optString("name"),
            classLabel = o.optNullableString("classLabel")?.cleanClassLabel(),
            // 老缓存里可能还带着职称，读出来时再清一次
            teacher = TeacherNames.clean(o.optNullableString("teacher")),
            room = o.optNullableString("room"),
            campus = o.optNullableString("campus"),
            dayOfWeek = o.optInt("day", 1),
            startSlot = o.optInt("startSlot", 1),
            endSlot = o.optInt("endSlot", 2),
            weeks = o.optJSONArray("weeks").toIntList(),
            rowIndex = o.optInt("rowIndex", 0),
        )
    }
}

/** 「无课表课程」：教务系统里已选但没有排上课表时间的课程。 */
data class UnscheduledCourse(
    val name: String,
    val classLabel: String? = null,
    val code: String? = null,
    val teacher: String? = null,
    val category: String? = null,
    val attribute: String? = null,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("name", name)
        put("classLabel", classLabel ?: JSONObject.NULL)
        put("code", code ?: JSONObject.NULL)
        put("teacher", teacher ?: JSONObject.NULL)
        put("category", category ?: JSONObject.NULL)
        put("attribute", attribute ?: JSONObject.NULL)
    }

    companion object {
        fun fromJson(o: JSONObject) = UnscheduledCourse(
            name = o.optString("name"),
            classLabel = o.optNullableString("classLabel")?.cleanClassLabel(),
            code = o.optNullableString("code"),
            teacher = TeacherNames.clean(o.optNullableString("teacher")),
            category = o.optNullableString("category"),
            attribute = o.optNullableString("attribute"),
        )
    }
}

/** 一次抓取得到的完整课表。 */
data class Schedule(
    val term: String = "",
    val termLabel: String = "",
    val courses: List<Course> = emptyList(),
    val unscheduled: List<UnscheduledCourse> = emptyList(),
    val slotRows: List<SlotRow> = SlotRow.DEFAULT,
    val maxWeek: Int = 20,
    /** 有课程安排的最大周次，用于生成周次选择器（避免出现一堆空白周）。 */
    val lastTeachingWeek: Int = 20,
    val fetchedAt: Long = 0L,
) {
    fun coursesAt(day: Int, rowIndex: Int, week: Int): List<Course> =
        courses.filter { it.dayOfWeek == day && it.rowIndex == rowIndex && it.activeIn(week) }

    fun coursesOnDay(day: Int, week: Int): List<Course> =
        courses.filter { it.dayOfWeek == day && it.activeIn(week) }
            .sortedBy { it.startSlot }

    /** 去重后的课程列表，用于「课程」页。 */
    val distinctCourses: List<Course>
        get() = courses.distinctBy {
            listOf(it.name, it.classLabel, it.teacher, it.room, it.dayOfWeek, it.startSlot, it.endSlot, it.weeks.joinToString(",")).joinToString("|")
        }

    fun toJson(): JSONObject = JSONObject().apply {
        put("term", term)
        put("termLabel", termLabel)
        put("maxWeek", maxWeek)
        put("lastTeachingWeek", lastTeachingWeek)
        put("fetchedAt", fetchedAt)
        put("courses", JSONArray().apply { courses.forEach { put(it.toJson()) } })
        put("unscheduled", JSONArray().apply { unscheduled.forEach { put(it.toJson()) } })
        put("slotRows", JSONArray().apply {
            slotRows.forEach { row ->
                put(JSONObject().put("label", row.label).put("start", row.startSlot).put("end", row.endSlot))
            }
        })
    }

    companion object {
        fun fromJson(o: JSONObject): Schedule {
            val rows = mutableListOf<SlotRow>()
            o.optJSONArray("slotRows")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val r = arr.optJSONObject(i) ?: continue
                    rows += SlotRow(r.optString("label"), r.optInt("start", 1), r.optInt("end", 2))
                }
            }
            return Schedule(
                term = o.optString("term"),
                termLabel = o.optString("termLabel"),
                courses = o.optJSONArray("courses").toObjectList { Course.fromJson(it) },
                unscheduled = o.optJSONArray("unscheduled").toObjectList { UnscheduledCourse.fromJson(it) },
                slotRows = rows.ifEmpty { SlotRow.DEFAULT },
                maxWeek = o.optInt("maxWeek", 20),
                lastTeachingWeek = o.optInt("lastTeachingWeek", o.optInt("maxWeek", 20)),
                fetchedAt = o.optLong("fetchedAt", 0L),
            )
        }

        fun parse(json: String?): Schedule? = try {
            json?.takeIf { it.isNotBlank() }?.let { fromJson(JSONObject(it)) }
        } catch (t: Throwable) {
            null
        }
    }
}

/** 去掉班级标识两边的括号（如 `(计算机1班)` -> `计算机1班`、`（足球）` -> `足球`）。 */
fun String.cleanClassLabel(): String =
    this.trim()
        .removePrefix("(")
        .removePrefix("（")
        .removeSuffix(")")
        .removeSuffix("）")
        .trim()

/** 把周次压缩成人类可读的文本：2..18 -> "2-18周"。 */
fun formatWeeks(weeks: List<Int>): String {
    if (weeks.isEmpty()) return "周次未知"
    val sorted = weeks.distinct().sorted()
    val parts = mutableListOf<String>()
    var i = 0
    while (i < sorted.size) {
        var j = i
        while (j + 1 < sorted.size && sorted[j + 1] == sorted[j] + 1) j++
        parts += if (j > i) "${sorted[i]}-${sorted[j]}" else "${sorted[i]}"
        i = j + 1
    }
    return parts.joinToString(",") + "周"
}

internal fun JSONObject.optNullableString(name: String): String? =
    if (isNull(name)) null else optString(name).takeIf { it.isNotBlank() && it != "null" }

internal fun JSONArray?.toIntList(): List<Int> {
    if (this == null) return emptyList()
    val out = mutableListOf<Int>()
    for (i in 0 until length()) out += optInt(i)
    return out
}

internal fun <T> JSONArray?.toObjectList(map: (JSONObject) -> T): List<T> {
    if (this == null) return emptyList()
    val out = mutableListOf<T>()
    for (i in 0 until length()) optJSONObject(i)?.let { out += map(it) }
    return out
}
