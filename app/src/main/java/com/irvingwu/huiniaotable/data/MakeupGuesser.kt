package com.irvingwu.huiniaotable.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/** 某个调休上班日补哪一天的课。[dayOfWeek] 用 1..7 表示周一..周日；null 表示不做补课映射。 */
data class MakeupRule(
    val date: LocalDate,
    val holidayName: String,
    val dayOfWeek: Int?,
)

/**
 * 动态推断「调休上班日补哪天的课」。
 *
 * 全国放假数据及各自治区数据里通常只有「哪天放假、哪天上班」，没有具体「执行周几课表」的官方映射字段。
 * 具体补哪天的课由学校/各地通知决定。
 *
 * 算法原理遵循中国大陆法定节假日与高校调课核心规则：
 * 1. 按节日名称与连续假期区间划分为独立的假期单元（例如元旦、春节、清明、劳动节、肉孜节等）；
 * 2. 统计该假期期间被借调占用的常规教学工作日（周一至周五，即 1..5）；
 * 3. 统计关联的调休上班日（周末被调为工作日的日期）：
 *    - **节前调休日**（调休日在放假前）：调休目的在于连接假期前段，依次补假期开始占用的前几个教学工作日（如第 1、2 个）；
 *    - **节后调休日**（调休日在放假后）：调休目的在于延长假期后段，依次补假期末尾占用的后几个教学工作日（如倒数第 2、1 个）；
 *    - 其他情况按时间距离最近的被借调工作日动态关联。
 *
 * 推断结果作为默认建议，用户可以在「节假日设置」中随时手动校准任何一天的补课星期。
 */
object MakeupGuesser {

    fun guess(days: Map<LocalDate, HolidayDay>): Map<LocalDate, Int> {
        if (days.isEmpty()) return emptyMap()

        val offDays = days.values.filter { it.isOffDay }.sortedBy { it.date }
        val workdays = days.values.filter { !it.isOffDay }.sortedBy { it.date }
        if (offDays.isEmpty() || workdays.isEmpty()) return emptyMap()

        // 1. 划分独立的放假段：优先按「连续日期」或者「相同节日名称且间隔 <= 3 天」聚合
        data class HolidayBlock(
            val name: String,
            val offDates: MutableList<LocalDate>,
            val makeupDates: MutableList<LocalDate> = mutableListOf(),
        )

        val blocks = mutableListOf<HolidayBlock>()
        for (day in offDays) {
            val last = blocks.lastOrNull()
            if (last != null && (last.offDates.last().plusDays(1) == day.date ||
                        (last.name == day.name && ChronoUnit.DAYS.between(last.offDates.last(), day.date) <= 3))) {
                last.offDates.add(day.date)
            } else {
                blocks.add(HolidayBlock(day.name, mutableListOf(day.date)))
            }
        }

        // 2. 将调休上班日关联到对应的假期块
        for (workday in workdays) {
            // 优先按相同节日名称关联（如果名称不是通用名称）
            val matchedByName = blocks.filter { block ->
                block.name.isNotBlank() &&
                        workday.name.isNotBlank() &&
                        (block.name == workday.name || block.name.contains(workday.name) || workday.name.contains(block.name)) &&
                        !workday.name.contains("调休") && !workday.name.contains("上班")
            }

            val targetBlock = if (matchedByName.isNotEmpty()) {
                matchedByName.minByOrNull { block ->
                    block.offDates.minOf { abs(ChronoUnit.DAYS.between(it, workday.date)) }
                } ?: blocks.first()
            } else {
                // 按日期绝对距离寻找最近的假期块
                blocks.minByOrNull { block ->
                    block.offDates.minOf { abs(ChronoUnit.DAYS.between(it, workday.date)) }
                }
            }

            targetBlock?.makeupDates?.add(workday.date)
        }

        // 3. 针对每个假期块，动态推算每个调休日对应的补课星期
        val result = linkedMapOf<LocalDate, Int>()
        for (block in blocks) {
            val swallowed = block.offDates.filter { it.dayOfWeek.value in 1..5 }.sorted()
            if (swallowed.isEmpty() || block.makeupDates.isEmpty()) continue

            val makeups = block.makeupDates.distinct().sorted()
            val blockStart = block.offDates.minOrNull() ?: continue
            val blockEnd = block.offDates.maxOrNull() ?: continue

            val before = makeups.filter { it.isBefore(blockStart) }
            val after = makeups.filter { it.isAfter(blockEnd) }
            val internal = makeups.filter { !it.isBefore(blockStart) && !it.isAfter(blockEnd) }

            // 节前调休：依次补假期起始阶段被占用的工作日
            before.forEachIndexed { index, date ->
                val target = swallowed.getOrNull(index.coerceIn(0, swallowed.lastIndex)) ?: swallowed.first()
                result[date] = target.dayOfWeek.value
            }

            // 节后调休：依次补假期结束阶段被占用的工作日（从末尾倒数匹配）
            after.forEachIndexed { index, date ->
                val targetIndex = (swallowed.size - after.size + index).coerceIn(0, swallowed.lastIndex)
                val target = swallowed[targetIndex]
                result[date] = target.dayOfWeek.value
            }

            // 假期内部调休（极少见）：按距离最近的被占用工作日
            internal.forEach { date ->
                val target = swallowed.minByOrNull { abs(ChronoUnit.DAYS.between(it, date)) } ?: swallowed.first()
                result[date] = target.dayOfWeek.value
            }
        }

        return result
    }

    /** 「周三」这样的短标签。 */
    fun weekdayLabel(dayOfWeek: Int): String =
        WEEKDAY_NAMES.getOrNull(dayOfWeek - 1)?.removePrefix("星期")?.let { "周$it" } ?: "—"
}
