package com.irvingwu.huiniaotable.notify

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.irvingwu.huiniaotable.MainActivity
import com.irvingwu.huiniaotable.R
import com.irvingwu.huiniaotable.data.AppState
import com.irvingwu.huiniaotable.data.TimetableRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * 上课实时通知（Live Updates）管理器。
 *
 * 与「今日课表」桌面控件、系统日历日程同源：都基于同一份课表状态（[TimetableRepository.state]）
 * 与开学日期推算。特性：
 *  1. **课前 15 分钟提醒**：每节课开始前 15 分钟推送一条高优先级通知（与系统日历日程的课前
 *     15 分钟提醒一致）；
 *  2. **上课实时进度**：上课期间常驻一条实时通知，展示当前课程、上课时间与教室，并用进度条
 *     表示本节课已进行的比例；Android 16（API 36）及以上自动升级为系统「实时更新 / Live Updates」；
 *  3. **自调度**：每次刷新后按今日的上课/下课时刻安排下一次唤醒，跨过时间点自动切换状态，
 *     无需 App 常驻后台；
 *  4. **节假日 / 调休自适应**：放假日不提醒，调休补课日按补课星期正常提醒。
 */
object ClassNotifier {

    /** 课前提醒提前量（分钟）。 */
    const val LEAD_MINUTES = 15

    const val ACTION_REFRESH = "com.irvingwu.huiniaotable.notify.REFRESH"

    private const val CHANNEL_REMINDER = "class_reminder"
    private const val CHANNEL_LIVE = "class_live"
    private const val LIVE_NOTIFICATION_ID = 0x1001
    private const val REMINDER_NOTIFICATION_ID = 0x1002
    private const val REFRESH_REQUEST = 0x2200
    private const val OPEN_APP_REQUEST = 0x2300

    /** Android 16 起支持 Live Updates（ProgressStyle + promoted ongoing）。 */
    private const val LIVE_UPDATE_API = 36

    /** 当前系统是否支持系统级「实时更新 / Live Updates」。 */
    val liveUpdatesSupported: Boolean
        get() = Build.VERSION.SDK_INT >= LIVE_UPDATE_API && isProgressStylePresent()

    private fun isProgressStylePresent(): Boolean {
        return try {
            Class.forName("android.app.Notification\$ProgressStyle")
            true
        } catch (t: Throwable) {
            false
        }
    }

    private const val PREFS_NAME = "class_notifier"
    private const val KEY_LAST_REMINDED = "last_reminded_start"

    private const val ACCENT_COLOR = 0xFF14663F.toInt()
    private const val PREVIEW_PROGRESS_PERCENT = 42

    private data class Occurrence(
        val name: String,
        val teacher: String?,
        val room: String?,
        val start: LocalDateTime,
        val end: LocalDateTime,
    ) {
        val timeText: String
            get() = "${formatTime(start.toLocalTime())}-${formatTime(end.toLocalTime())}"

        /**
         * 通知第二行文案：上课时间 · 授课教师 · 教室。
         * 校区信息已替换为授课教师，同时保留教室，三者按存在的部分依次拼接。
         */
        val detailText: String
            get() = buildList {
                add(timeText)
                teacher?.takeIf { it.isNotBlank() }?.let { add(it) }
                room?.takeIf { it.isNotBlank() }?.let { add(it) }
            }.joinToString(" · ")
    }

    /** 是否已获得通知权限（Android 13 起需要 POST_NOTIFICATIONS）。 */
    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * 重新计算今日课程并同步通知与下一次唤醒闹钟。
     * 课表、周次、开学日期、节假日或开关变化后都应调用一次。
     */
    fun refresh(context: Context) {
        val app = context.applicationContext
        try {
            TimetableRepository.init(app)
            val state = TimetableRepository.state.value
            val manager = app.getSystemService(NotificationManager::class.java) ?: return

            if (!state.classNotify || !canPost(app)) {
                manager.cancel(LIVE_NOTIFICATION_ID)
                manager.cancel(REMINDER_NOTIFICATION_ID)
                cancelNextRefresh(app)
                return
            }

            // 没有课表或尚未设置开学日期时无需通知，也不要留下空转的唤醒闹钟。
            if (state.schedule == null || state.needsTermStart) {
                manager.cancel(LIVE_NOTIFICATION_ID)
                manager.cancel(REMINDER_NOTIFICATION_ID)
                cancelNextRefresh(app)
                return
            }

            ensureChannels(app)

            val now = LocalDateTime.now()
            val occurrences = occurrencesFor(state, LocalDate.now())
            val current = occurrences.firstOrNull { !now.isBefore(it.start) && now.isBefore(it.end) }
            val upcoming = occurrences.firstOrNull {
                it.start.isAfter(now) && !it.start.isAfter(now.plusMinutes(LEAD_MINUTES.toLong()))
            }

            // 课前 15 分钟提醒（每节课只提醒一次）
            if (upcoming != null) {
                val prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val startEpoch = epochMillis(upcoming.start)
                if (prefs.getLong(KEY_LAST_REMINDED, 0L) != startEpoch) {
                    postReminder(app, upcoming, ChronoUnit.MINUTES.between(now, upcoming.start))
                    prefs.edit().putLong(KEY_LAST_REMINDED, startEpoch).apply()
                }
            }

            // 上课中的实时进度通知
            if (current != null) {
                postLiveUpdate(app, current, progressPercent(current, now))
            } else {
                manager.cancel(LIVE_NOTIFICATION_ID)
            }

            scheduleNextRefresh(app, occurrences, now)
        } catch (t: Throwable) {
            // 防御性捕获，避免任何后台状态调度异常导致应用崩溃
        }
    }

    /**
     * 调试用：立即推送一条「正在上课」的实时更新预览，不依赖当前时间与课表。
     * 若课表里正好有进行中 / 待上课的课程就用它，否则用一节演示课程。
     *
     * @return 面向用户的结果描述（用于 Snackbar 提示）。
     */
    fun previewLiveUpdate(context: Context): String {
        return try {
            val app = context.applicationContext
            if (!canPost(app)) return "未授予通知权限，无法发送实时更新预览"
            TimetableRepository.init(app)
            ensureChannels(app)
            val occurrence = previewOccurrence()
            postLiveUpdate(app, occurrence, PREVIEW_PROGRESS_PERCENT)
            "已发送实时更新预览：${occurrence.name}"
        } catch (t: Throwable) {
            "发送实时更新预览失败：${t.localizedMessage ?: t.javaClass.simpleName}"
        }
    }

    /** 调试用：立即推送一条「课前 15 分钟」提醒预览。 */
    fun previewReminder(context: Context): String {
        return try {
            val app = context.applicationContext
            if (!canPost(app)) return "未授予通知权限，无法发送提醒预览"
            TimetableRepository.init(app)
            ensureChannels(app)
            val occurrence = previewOccurrence()
            postReminder(app, occurrence, LEAD_MINUTES.toLong())
            "已发送课前提醒预览：${occurrence.name}"
        } catch (t: Throwable) {
            "发送课前提醒预览失败：${t.localizedMessage ?: t.javaClass.simpleName}"
        }
    }

    /** 调试用：清除本应用发出的实时通知与提醒。 */
    fun clearNotifications(context: Context) {
        try {
            val manager = context.applicationContext.getSystemService(NotificationManager::class.java)
            manager?.cancel(LIVE_NOTIFICATION_ID)
            manager?.cancel(REMINDER_NOTIFICATION_ID)
        } catch (t: Throwable) {}
    }

    private fun previewOccurrence(): Occurrence {
        val state = TimetableRepository.state.value
        val now = LocalDateTime.now()
        val today = LocalDate.now()
        if (state.schedule != null && !state.needsTermStart && !state.isOffDay(today)) {
            val list = occurrencesFor(state, today)
            list.firstOrNull { !now.isBefore(it.start) && now.isBefore(it.end) }?.let { return it }
            list.firstOrNull { it.start.isAfter(now) }?.let { return it }
        }
        return Occurrence(
            name = "大学英语",
            teacher = "李老师",
            room = "第一教学楼 102",
            start = now.minusMinutes(10),
            end = now.plusMinutes(35),
        )
    }

    private fun occurrencesFor(state: AppState, date: LocalDate): List<Occurrence> {
        val schedule = state.schedule ?: return emptyList()
        if (state.needsTermStart || state.isOffDay(date)) return emptyList()
        val week = state.currentWeek
        val day = state.displayDayOfWeek(date, week)
        return schedule.coursesOnDay(day, week)
            .mapNotNull { course ->
                val range = parseRange(state.timeTextFor(course)) ?: return@mapNotNull null
                Occurrence(
                    name = course.name,
                    teacher = course.teacher,
                    room = course.room,
                    start = date.atTime(range.first),
                    end = date.atTime(range.second),
                )
            }
            .sortedBy { it.start }
    }

    private fun postReminder(context: Context, occurrence: Occurrence, minutesUntilStart: Long) {
        try {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            val minutes = minutesUntilStart.coerceAtLeast(0)
            val body = occurrence.detailText
            val notification = Notification.Builder(context, CHANNEL_REMINDER)
                .setSmallIcon(R.drawable.ic_notify)
                .setContentTitle("还有 ${minutes} 分钟上课 · ${occurrence.name}")
                .setContentText(body)
                .setStyle(Notification.BigTextStyle().bigText("${occurrence.name}\n$body"))
                .setContentIntent(openAppPendingIntent(context))
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_REMINDER)
                .build()

            manager.notify(REMINDER_NOTIFICATION_ID, notification)
        } catch (t: Throwable) {}
    }

    private fun postLiveUpdate(context: Context, occurrence: Occurrence, percent: Int) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        var liveStyleApplied = false
        val builder = Notification.Builder(context, CHANNEL_LIVE)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle("正在上课 · ${occurrence.name}")
            .setContentText(occurrence.detailText)
            .setContentIntent(openAppPendingIntent(context))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(Notification.CATEGORY_PROGRESS)

        if (Build.VERSION.SDK_INT >= LIVE_UPDATE_API) {
            try {
                liveStyleApplied = Api36Impl.applyProgressStyle(context, builder, percent, ACCENT_COLOR)
            } catch (t: Throwable) {
                liveStyleApplied = false
            }
        }

        if (!liveStyleApplied) {
            builder.setProgress(100, percent, false)
        }

        try {
            manager.notify(LIVE_NOTIFICATION_ID, builder.build())
        } catch (t: Throwable) {
            // 若包含 ProgressStyle 的通知在某些定制 ROM 的 NotificationManager.notify 校验中抛出异常，
            // 降级为最基础的进行中通知重新发送，保证不崩溃且通知可见。
            if (liveStyleApplied) {
                try {
                    val fallbackBuilder = Notification.Builder(context, CHANNEL_LIVE)
                        .setSmallIcon(R.drawable.ic_notify)
                        .setContentTitle("正在上课 · ${occurrence.name}")
                        .setContentText(occurrence.detailText)
                        .setContentIntent(openAppPendingIntent(context))
                        .setOngoing(true)
                        .setOnlyAlertOnce(true)
                        .setShowWhen(false)
                        .setCategory(Notification.CATEGORY_PROGRESS)
                        .setProgress(100, percent, false)
                    manager.notify(LIVE_NOTIFICATION_ID, fallbackBuilder.build())
                } catch (ignored: Throwable) {}
            }
        }
    }

    @RequiresApi(LIVE_UPDATE_API)
    private object Api36Impl {
        fun applyProgressStyle(
            context: Context,
            builder: Notification.Builder,
            percent: Int,
            accentColor: Int,
        ): Boolean {
            return try {
                // 进度条上的圆形跟踪球（tracker icon）——即 Google 文档里进度条上那个「圆球」。
                // 缺少它时进度条只显示分段色，不会有圆形指示物。
                val trackerIcon = Icon.createWithResource(context, R.drawable.ic_live_tracker)
                val segment = Notification.ProgressStyle.Segment(100)
                    .setColor(accentColor)
                val style = Notification.ProgressStyle()
                    .setProgress(percent)
                    .setProgressSegments(listOf(segment))
                    .setProgressTrackerIcon(trackerIcon)
                builder.setStyle(style)
                builder.setColor(accentColor)

                // Android 16 Rich Ongoing / Live Updates 相关 Builder API（在小米 HyperOS 等厂商 ROM 上可能未包含或未启用），
                // 通过反射调用，若不存在则安全跳过，防止抛出 NoSuchMethodError 造成崩溃。
                safeInvokeBuilder(builder, "setRequestPromotedOngoing", Boolean::class.javaPrimitiveType, true)
                safeInvokeBuilder(builder, "setShortCriticalText", CharSequence::class.java, "上课中")
                true
            } catch (t: Throwable) {
                false
            }
        }

        private fun safeInvokeBuilder(
            builder: Notification.Builder,
            methodName: String,
            paramType: Class<*>?,
            paramValue: Any?,
        ) {
            try {
                val method = Notification.Builder::class.java.getMethod(methodName, paramType)
                method.invoke(builder, paramValue)
            } catch (t: Throwable) {
                // 忽略厂商 ROM 未包含的 API
            }
        }
    }

    private fun progressPercent(occurrence: Occurrence, now: LocalDateTime): Int {
        val total = ChronoUnit.SECONDS.between(occurrence.start, occurrence.end)
        if (total <= 0) return 100
        val elapsed = ChronoUnit.SECONDS.between(occurrence.start, now)
        return (elapsed * 100 / total).toInt().coerceIn(0, 100)
    }

    /** 按今日的上课 / 下课 / 课前 15 分钟时刻安排下一次唤醒；当天没有课时退回明天零点。 */
    private fun scheduleNextRefresh(
        context: Context,
        occurrences: List<Occurrence>,
        now: LocalDateTime,
    ) {
        val boundaries = ArrayList<LocalDateTime>(occurrences.size * 3)
        occurrences.forEach {
            boundaries += it.start.minusMinutes(LEAD_MINUTES.toLong())
            boundaries += it.start
            boundaries += it.end
        }
        val fallback = LocalDate.now().plusDays(1).atStartOfDay()
        val next = boundaries.filter { it.isAfter(now) }.minOrNull() ?: fallback
        val triggerAt = maxOf(next, now.plusSeconds(30))
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        setAlarm(context, triggerAt)
    }

    private fun setAlarm(context: Context, triggerAt: Long) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = refreshPendingIntent(context)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            } else {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
        } catch (se: SecurityException) {
            // 小米 / MIUI 等系统或 Android 12+ 限制精确闹钟权限时降级处理
            try {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            } catch (t: Throwable) {
                try {
                    manager.set(AlarmManager.RTC_WAKEUP, triggerAt, pending)
                } catch (ignored: Throwable) {}
            }
        } catch (t: Throwable) {
            try {
                manager.set(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            } catch (ignored: Throwable) {}
        }
    }

    private fun cancelNextRefresh(context: Context) {
        try {
            val manager = context.getSystemService(AlarmManager::class.java)
            manager?.cancel(refreshPendingIntent(context))
        } catch (t: Throwable) {}
    }

    private fun refreshPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ClassNotifierReceiver::class.java).setAction(ACTION_REFRESH)
        return PendingIntent.getBroadcast(
            context,
            REFRESH_REQUEST,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun openAppPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            OPEN_APP_REQUEST,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun ensureChannels(context: Context) {
        try {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            if (manager.getNotificationChannel(CHANNEL_REMINDER) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_REMINDER,
                        "上课提醒",
                        NotificationManager.IMPORTANCE_HIGH,
                    ).apply { description = "课前 15 分钟提醒即将开始的课程" },
                )
            }
            if (manager.getNotificationChannel(CHANNEL_LIVE) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_LIVE,
                        "上课实时状态",
                        NotificationManager.IMPORTANCE_LOW,
                    ).apply {
                        description = "上课期间常驻显示当前课程与进度"
                        setShowBadge(false)
                    },
                )
            }
        } catch (t: Throwable) {}
    }

    private fun parseRange(text: String): Pair<LocalTime, LocalTime>? {
        val parts = text.split('-')
        if (parts.size != 2) return null
        val start = runCatching { LocalTime.parse(parts[0].trim()) }.getOrNull() ?: return null
        val end = runCatching { LocalTime.parse(parts[1].trim()) }.getOrNull() ?: return null
        return start to end
    }

    private fun epochMillis(dateTime: LocalDateTime): Long =
        dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun formatTime(time: LocalTime): String =
        "%02d:%02d".format(time.hour, time.minute)
}
