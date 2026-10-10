package com.irvingwu.huiniaotable.data

import android.Manifest
import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.TimeZone

/**
 * 系统日历日程同步管理器：负责将学期全部课程批量写入 Android 系统日历（Calendar Provider）。
 *
 * 特性：
 *  1. 全学期全量导入：依课表周次覆盖全部周次（如 1~18 周）；
 *  2. 幂等更新与一键清除：提供全量清除与覆写机制，通过专属标签隔离本应用写入的日程；
 *  3. 精准时段：自动结合教室所属楼栋的时间表，生成毫秒级起止时间（DTSTART / DTEND）；
 *  4. 调休适配：自动跳过法定节假日停课日，并将调休教学日的补课安排写入对应公历日期；
 *  5. 系统级提醒：每节课自动附加课前 15 分钟的系统级日历警报提醒（Reminders）；
 *  6. 桌面小组件支持：写入系统日历后，系统自带的日历应用与负一屏/桌面日历小组件均可原生直接读取展示。
 */
object CalendarSyncManager {

    private const val SYNC_TAG = "[由灰鸟课表同步]"
    private const val ACCOUNT_NAME = "HuiniaoTimetable"

    fun hasCalendarPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CALENDAR,
        ) == PackageManager.PERMISSION_GRANTED

    private data class ClassOccurrence(
        val title: String,
        val location: String,
        val description: String,
        val startMillis: Long,
        val endMillis: Long,
        val timeZoneId: String,
    )

    /**
     * 将课表全部日程写入系统日历。
     *
     * @return 成功写入的课程节数；如果因未设置开学日期等原因中断，返回 0。
     */
    suspend fun syncToSystemCalendar(
        context: Context,
        state: AppState,
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val schedule = state.schedule ?: return@runCatching 0
            val termStart = state.termStart ?: throw IllegalStateException("未设定学期开学首周基准日，无法确定公历时间")

            val resolver = context.contentResolver
            val calendarId = getOrCreateCalendarId(context)

            // 1. 先清理此前由本应用写入的全部历史课程日程，避免重复堆叠
            clearAllSyncedEvents(context)

            // 2. 统计并收集全部学期课程事件
            val zoneId = ZoneId.systemDefault()
            val timeZoneId = TimeZone.getDefault().id
            val maxCourseWeek = schedule.courses.flatMap { it.weeks }.maxOrNull() ?: state.lastTeachingWeek
            val totalWeeks = maxOf(maxCourseWeek, state.lastTeachingWeek).coerceAtLeast(state.currentWeek)

            val occurrences = ArrayList<ClassOccurrence>()

            for (week in 1..totalWeeks) {
                for (dayIndex in 1..7) {
                    val date = dateOf(dayIndex, week, termStart)

                    // 法定节假日或自定义放假日停课，不写入日程
                    if (state.isOffDay(date)) continue

                    // 调休上班日可能按另一星期的课表补课
                    val effectiveDay = state.displayDayOfWeek(date, week)
                    val courses = schedule.coursesAtDayOnly(effectiveDay, week)

                    for (course in courses) {
                        val scheme = state.schemeFor(course)
                        val periods = ClassTimes.periods(scheme)
                        val startSlotIdx = (course.startSlot - 1).coerceIn(0, periods.size - 1)
                        val endSlotIdx = (course.endSlot - 1).coerceIn(0, periods.size - 1)

                        val startTime = runCatching { LocalTime.parse(periods[startSlotIdx].start) }
                            .getOrNull() ?: LocalTime.of(10, 0)
                        val endTime = runCatching { LocalTime.parse(periods[endSlotIdx].end) }
                            .getOrNull() ?: LocalTime.of(11, 40)

                        val startMillis = date.atTime(startTime).atZone(zoneId).toInstant().toEpochMilli()
                        val endMillis = date.atTime(endTime).atZone(zoneId).toInstant().toEpochMilli()

                        val description = buildString {
                            course.teacher?.let { append("主讲教师: $it\n") }
                            course.room?.let { append("上课场地: $it\n") }
                            course.classLabel?.let { append("教学班级: $it\n") }
                            append("时段安排: 第 ${course.slotText} (${periods[startSlotIdx].start}–${periods[endSlotIdx].end})\n")
                            append("教学周次: 第 $week 周 (全课程 ${course.weekText})\n")
                            append("作息规范: ${scheme.fullLabel}\n")
                            state.makeupLabelFor(date, week)?.let { append("调休说明: 调休教学日 ($it)\n") }
                            append(SYNC_TAG)
                        }

                        occurrences.add(
                            ClassOccurrence(
                                title = course.name,
                                location = course.room.orEmpty(),
                                description = description,
                                startMillis = startMillis,
                                endMillis = endMillis,
                                timeZoneId = timeZoneId,
                            ),
                        )
                    }
                }
            }

            // 3. 分批构建并提交操作：每批 40 节（含提醒共 80 个操作），
            // 提醒的外键回溯引用严格限定在当前 batch 内的相对下标（0, 2, 4...），杜绝越界
            var totalInserted = 0
            occurrences.chunked(40).forEach { batch ->
                val ops = ArrayList<ContentProviderOperation>()
                batch.forEach { item ->
                    val eventIndexInBatch = ops.size
                    ops.add(
                        ContentProviderOperation.newInsert(CalendarContract.Events.CONTENT_URI)
                            .withValue(CalendarContract.Events.CALENDAR_ID, calendarId)
                            .withValue(CalendarContract.Events.TITLE, item.title)
                            .withValue(CalendarContract.Events.EVENT_LOCATION, item.location)
                            .withValue(CalendarContract.Events.DESCRIPTION, item.description)
                            .withValue(CalendarContract.Events.DTSTART, item.startMillis)
                            .withValue(CalendarContract.Events.DTEND, item.endMillis)
                            .withValue(CalendarContract.Events.EVENT_TIMEZONE, item.timeZoneId)
                            .withValue(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CONFIRMED)
                            .withValue(CalendarContract.Events.HAS_ALARM, 1)
                            .build(),
                    )
                    ops.add(
                        ContentProviderOperation.newInsert(CalendarContract.Reminders.CONTENT_URI)
                            .withValueBackReference(CalendarContract.Reminders.EVENT_ID, eventIndexInBatch)
                            .withValue(CalendarContract.Reminders.MINUTES, 15)
                            .withValue(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                            .build(),
                    )
                }
                resolver.applyBatch(CalendarContract.AUTHORITY, ops)
                totalInserted += batch.size
            }

            totalInserted
        }
    }

    /**
     * 一键清除此前由本应用同步写入系统日历的全部课程日程。
     *
     * @return 成功清理的日程条目数量
     */
    suspend fun clearAllSyncedEvents(context: Context): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            if (!hasCalendarPermission(context)) {
                throw SecurityException("未授予系统日历写入权限")
            }
            val resolver = context.contentResolver
            val deletedCount = resolver.delete(
                CalendarContract.Events.CONTENT_URI,
                "${CalendarContract.Events.DESCRIPTION} LIKE ?",
                arrayOf("%$SYNC_TAG%"),
            )
            deletedCount
        }
    }

    private fun dateOf(dayIndex: Int, week: Int, termStart: LocalDate): LocalDate {
        val monday = termStart.minusDays((termStart.dayOfWeek.value - 1).toLong())
        return monday.plusWeeks((week - 1).toLong()).plusDays((dayIndex - 1).toLong())
    }

    private fun Schedule.coursesAtDayOnly(dayOfWeek: Int, week: Int): List<Course> =
        courses.filter { it.dayOfWeek == dayOfWeek && it.activeIn(week) }

    private fun getOrCreateCalendarId(context: Context): Long {
        val resolver = context.contentResolver
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.VISIBLE,
        )

        // 优先使用设备现有主日历或可用日历
        runCatching {
            resolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                null,
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(CalendarContract.Calendars._ID)
                val primaryIdx = cursor.getColumnIndex(CalendarContract.Calendars.IS_PRIMARY)
                var fallbackId = -1L

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIdx)
                    if (primaryIdx != -1 && cursor.getInt(primaryIdx) == 1) {
                        return id
                    }
                    if (fallbackId == -1L) {
                        fallbackId = id
                    }
                }
                if (fallbackId != -1L) return fallbackId
            }
        }

        // 无可用日历时创建应用专属本地日历
        return createLocalCalendar(context)
    }

    private fun createLocalCalendar(context: Context): Long {
        val uri = CalendarContract.Calendars.CONTENT_URI.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, ACCOUNT_NAME)
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            .build()

        val values = ContentValues().apply {
            put(CalendarContract.Calendars.ACCOUNT_NAME, ACCOUNT_NAME)
            put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            put(CalendarContract.Calendars.NAME, "灰鸟课表")
            put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, "灰鸟课表")
            put(CalendarContract.Calendars.CALENDAR_COLOR, 0xFF14663F.toInt())
            put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER)
            put(CalendarContract.Calendars.OWNER_ACCOUNT, ACCOUNT_NAME)
            put(CalendarContract.Calendars.VISIBLE, 1)
            put(CalendarContract.Calendars.SYNC_EVENTS, 1)
        }

        val resultUri = context.contentResolver.insert(uri, values)
        return resultUri?.lastPathSegment?.toLongOrNull() ?: 1L
    }
}
