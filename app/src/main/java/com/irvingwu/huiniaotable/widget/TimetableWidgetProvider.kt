package com.irvingwu.huiniaotable.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.irvingwu.huiniaotable.MainActivity
import com.irvingwu.huiniaotable.R
import com.irvingwu.huiniaotable.data.AppState
import com.irvingwu.huiniaotable.data.Course
import com.irvingwu.huiniaotable.data.Schedule
import com.irvingwu.huiniaotable.data.TimetableRepository
import com.irvingwu.huiniaotable.data.WEEKDAY_NAMES
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

private const val ACTION_REFRESH = "com.irvingwu.huiniaotable.widget.REFRESH"
private const val REQUEST_REFRESH = 4096

/** 一行课程相对「现在」的状态，决定配色。 */
private enum class RowStatus { PAST, ACTIVE, UPCOMING }

private data class TimeRange(val start: LocalTime, val end: LocalTime)

/**
 * 「今日课表」桌面控件。
 *
 * 采用 RemoteViews 实现，不依赖 Compose；配色直接引用系统的 Material You 动态取色
 * 语义色（壁纸配色），与 App 内的 dynamicLight/DarkColorScheme 同源，浅色/深色自适应。
 *
 * 三种课程状态：
 *  - 已结束（下课时间已过）：灰色弱化；
 *  - 正在上课 / 接下来一节：强调色（primary）；
 *  - 尚未开始：常规前景色（深色主题下即白色）。
 *
 * 控件支持横竖自由缩放，展示行数按当前高度自适应裁剪；每次渲染后会按今天的
 * 上课/下课时刻安排下一次刷新，保证状态着色及时切换。
 */
class TimetableWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { renderWidget(context, appWidgetManager, it) }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        renderWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onEnabled(context: Context) {
        renderAllWidgets(context)
    }

    override fun onDisabled(context: Context) {
        cancelRefresh(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            -> renderAllWidgets(context)
        }
    }

    companion object {
        /** App 侧在课表刷新 / 回到前台时调用，立即重绘所有控件。 */
        fun updateAll(context: Context) = renderAllWidgets(context.applicationContext)
    }
}

private fun renderAllWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val ids = manager.getAppWidgetIds(ComponentName(context, TimetableWidgetProvider::class.java))
    ids.forEach { renderWidget(context, manager, it) }
}

private fun renderWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
    val views = try {
        buildWidgetViews(context, manager, widgetId)
    } catch (t: Throwable) {
        fallbackWidgetViews(context)
    }
    manager.updateAppWidget(widgetId, views)
    runCatching { scheduleNextRefresh(context) }
}

private fun buildWidgetViews(
    context: Context,
    manager: AppWidgetManager,
    widgetId: Int,
): RemoteViews {
    TimetableRepository.init(context)
    val state = TimetableRepository.state.value

    val views = RemoteViews(context.packageName, R.layout.widget_timetable)
    views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent(context))

    val today = LocalDate.now()
    val holiday = state.holidayOn(today)
    val offDay = holiday?.isOffDay == true
    val makeupLabel = state.makeupLabelFor(today, state.currentWeek)

    views.setTextViewText(R.id.widget_title, context.getString(R.string.widget_title))
    views.setTextViewText(
        R.id.widget_subtitle,
        subtitleText(state, today, holiday?.name, offDay, makeupLabel),
    )

    views.removeAllViews(R.id.widget_course_list)

    val schedule = state.schedule
    val courses = if (schedule != null && !state.needsTermStart && !offDay) {
        val day = state.displayDayOfWeek(today, state.currentWeek)
        schedule.coursesOnDay(day, state.currentWeek)
    } else {
        emptyList()
    }

    val rows = if (courses.isEmpty()) {
        emptyList()
    } else {
        buildRowViews(context, state, courses, availableRows(manager, widgetId))
    }

    if (rows.isEmpty()) {
        views.setViewVisibility(R.id.widget_course_list, View.GONE)
        views.setViewVisibility(R.id.widget_empty, View.VISIBLE)
        views.setTextViewText(R.id.widget_empty, emptyText(state, schedule, offDay))
    } else {
        views.setViewVisibility(R.id.widget_course_list, View.VISIBLE)
        views.setViewVisibility(R.id.widget_empty, View.GONE)
        rows.forEach { views.addView(R.id.widget_course_list, it) }
    }

    return views
}

/** 构造失败时的兜底：只用主布局 + 一行提示，避免宿主显示「载入失败」。 */
private fun fallbackWidgetViews(context: Context): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_timetable)
    views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent(context))
    views.setTextViewText(R.id.widget_title, context.getString(R.string.widget_title))
    views.setTextViewText(R.id.widget_subtitle, "打开 App 同步课表")
    views.setViewVisibility(R.id.widget_course_list, View.GONE)
    views.setViewVisibility(R.id.widget_empty, View.VISIBLE)
    views.setTextViewText(R.id.widget_empty, "打开 App 同步课表")
    return views
}

/** 按控件当前高度估算能放下几行，最少 2 行、最多 8 行。 */
private fun availableRows(manager: AppWidgetManager, widgetId: Int): Int {
    val heightDp = manager.getAppWidgetOptions(widgetId)
        .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
    return ((heightDp - 46) / 31).coerceIn(2, 8)
}

private fun buildRowViews(
    context: Context,
    state: AppState,
    courses: List<Course>,
    maxRows: Int,
): List<RemoteViews> {
    val now = LocalTime.now()
    val ranges = courses.map { parseRange(state.timeTextFor(it)) }
    val currentIndex = ranges.indexOfFirst {
        it != null && !it.start.isAfter(now) && now.isBefore(it.end)
    }
    val nextIndex = ranges.indexOfFirst { it != null && it.start.isAfter(now) }

    val truncated = courses.size > maxRows
    val shownCount = if (truncated) (maxRows - 1).coerceAtLeast(0) else courses.size

    val rows = ArrayList<RemoteViews>(shownCount + 1)
    for (index in 0 until shownCount) {
        val range = ranges[index]
        val status = when {
            range != null && !range.end.isAfter(now) -> RowStatus.PAST
            index == currentIndex || index == nextIndex -> RowStatus.ACTIVE
            else -> RowStatus.UPCOMING
        }
        rows += courseRow(context, state, courses[index], status)
    }

    if (truncated) {
        val footer = RemoteViews(context.packageName, R.layout.widget_footer)
        footer.setTextViewText(
            R.id.widget_footer_text,
            "还有 ${courses.size - shownCount} 门课程",
        )
        rows += footer
    }
    return rows
}

private fun courseRow(
    context: Context,
    state: AppState,
    course: Course,
    status: RowStatus,
): RemoteViews {
    val row = RemoteViews(context.packageName, R.layout.widget_course_row)
    val titleColor = when (status) {
        RowStatus.PAST -> context.getColor(R.color.widget_muted)
        RowStatus.ACTIVE -> context.getColor(R.color.widget_accent)
        RowStatus.UPCOMING -> context.getColor(R.color.widget_foreground)
    }

    row.setTextViewText(R.id.widget_row_title, course.name)
    row.setTextColor(R.id.widget_row_title, titleColor)
    row.setTextViewText(R.id.widget_row_sub, courseSubtitle(state, course))
    row.setTextColor(R.id.widget_row_sub, context.getColor(R.color.widget_muted))
    row.setInt(R.id.widget_row_bar, "setBackgroundColor", titleColor)
    row.setFloat(R.id.widget_row, "setAlpha", if (status == RowStatus.PAST) 0.55f else 1f)
    return row
}

private fun courseSubtitle(state: AppState, course: Course): String = buildList {
    state.timeTextFor(course).takeIf { it.isNotBlank() }?.let { add(it) }
    course.room?.takeIf { it.isNotBlank() }?.let { add(it) }
}.joinToString(" · ")

private fun subtitleText(
    state: AppState,
    today: LocalDate,
    holidayName: String?,
    offDay: Boolean,
    makeupLabel: String?,
): String {
    val weekday = WEEKDAY_NAMES.getOrElse(today.dayOfWeek.value - 1) { "" }
    val dateText = "${today.monthValue}月${today.dayOfMonth}日 $weekday"
    return when {
        offDay -> "$dateText · 法定节假日${holidayName?.let { " $it" } ?: ""} · 休息"
        state.needsTermStart -> "$dateText · 未设置开学日期"
        makeupLabel != null -> "$dateText · $makeupLabel · 第${state.currentWeek}周"
        else -> "$dateText · 第${state.currentWeek}周"
    }
}

private fun emptyText(state: AppState, schedule: Schedule?, offDay: Boolean): String = when {
    schedule == null -> "打开 App 同步课表"
    state.needsTermStart -> "打开 App 设置开学日期"
    offDay -> "今日休息"
    else -> "今日无课"
}

private fun parseRange(text: String): TimeRange? {
    val parts = text.split('-')
    if (parts.size != 2) return null
    val start = runCatching { LocalTime.parse(parts[0].trim()) }.getOrNull() ?: return null
    val end = runCatching { LocalTime.parse(parts[1].trim()) }.getOrNull() ?: return null
    return TimeRange(start, end)
}

private fun openAppPendingIntent(context: Context): PendingIntent {
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    return PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

/**
 * 按今天的上课 / 下课时刻安排下一次重绘，保证「正在上课 / 接下来一节 / 已结束」
 * 的配色能准时切换；没有 Widget 或当天没有课时，退回明天零点。
 */
private fun scheduleNextRefresh(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val ids = manager.getAppWidgetIds(ComponentName(context, TimetableWidgetProvider::class.java))
    if (ids.isEmpty()) return

    val state = TimetableRepository.state.value
    val now = LocalDateTime.now()
    val today = now.toLocalDate()

    val boundaries = mutableListOf<LocalDateTime>()
    val schedule = state.schedule
    if (schedule != null && !state.needsTermStart && !state.isOffDay(today)) {
        val day = state.displayDayOfWeek(today, state.currentWeek)
        schedule.coursesOnDay(day, state.currentWeek).forEach { course ->
            parseRange(state.timeTextFor(course))?.let { range ->
                boundaries += LocalDateTime.of(today, range.start)
                boundaries += LocalDateTime.of(today, range.end)
            }
        }
    }

    val fallback = today.plusDays(1).atStartOfDay()
    val trigger = boundaries.filter { it.isAfter(now) }.minOrNull() ?: fallback
    val triggerAt = maxOf(trigger, now.plusSeconds(30))
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarmManager.set(AlarmManager.RTC, triggerAt, refreshPendingIntent(context))
}

private fun cancelRefresh(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarmManager.cancel(refreshPendingIntent(context))
}

private fun refreshPendingIntent(context: Context): PendingIntent {
    val intent = Intent(context, TimetableWidgetProvider::class.java).setAction(ACTION_REFRESH)
    return PendingIntent.getBroadcast(
        context,
        REQUEST_REFRESH,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
